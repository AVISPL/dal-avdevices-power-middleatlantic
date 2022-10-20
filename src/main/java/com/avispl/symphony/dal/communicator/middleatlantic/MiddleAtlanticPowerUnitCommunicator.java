/*
 * Copyright (c) 2020-2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.middleatlantic;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.communicator.middleatlantic.statistics.DynamicStatisticsDefinitions;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static com.avispl.symphony.dal.util.ControllablePropertyFactory.*;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Middle Atlantic Premium+ PDU Adapter.
 * Uses http communication to provide the following information:
 * - Inlet Active Power [float, ex 504.985024, historical]
 * - Inlet Line Frequency [float, ex 59.999999, historical]
 * - Inlet RMS Current [float, ex 4.7, historical]
 * - Inlet RMS Voltage [float, ex 115.119997, historical]
 * - Outlet 1-8 [switch, controllable property]
 * - Outlet 1-8 RMS Current [float, ex 2.965, historical]
 *
 * @author Maksym.Rossiitsev / Symphony Dev Team<br>
 * @since 1.0
 */
public class MiddleAtlanticPowerUnitCommunicator extends RestCommunicator implements Monitorable, Controller {

    private static final String OUTLET = "Outlet";
    private static final String BASE_URI = "model/pdu/0";
    private static final String GET_READING = "getReading";
    private ExtendedStatistics localStatistics;
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ReentrantLock reentrantLock = new ReentrantLock();

    private volatile boolean controlsRunning = false;
    private Set<String> historicalProperties = new HashSet<>();

    /**
     * Retrieves {@link #historicalProperties}
     *
     * @return value of {@link #historicalProperties}
     */
    public String getHistoricalProperties() {
        return String.join(",", this.historicalProperties);
    }

    /**
     * Sets {@link #historicalProperties} value
     *
     * @param historicalProperties new value of {@link #historicalProperties}
     */
    public void setHistoricalProperties(String historicalProperties) {
        this.historicalProperties.clear();
        Arrays.asList(historicalProperties.split(",")).forEach(propertyName -> {
            this.historicalProperties.add(propertyName.trim());
        });
    }

    /**
     * MiddleAtlanticPowerUnit constructor.
     */
    public MiddleAtlanticPowerUnitCommunicator() {
        super();
        // override default value to trust all certificates - power unit typically do not have trusted certificates installed
        // it can be changed back by configuration
        setTrustAllCertificates(true);
    }

    @Override
    protected void authenticate() throws Exception {
        // nothing to do here, authentication is done in individual requests
    }

    /**
     * Getting statistics of the device.
     * Need to make sure that none of control operations are running when trying to fetch the statistics
     */
    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        // This is to make sure if the statistics is being fetched before/after any set of control operations

        reentrantLock.lock();
        try {
            if(controlsRunning && localStatistics != null){
                extendedStatistics.setStatistics(new HashMap<>(localStatistics.getStatistics()));
                extendedStatistics.setControllableProperties(localStatistics.getControllableProperties());
                return Collections.singletonList(extendedStatistics);
            }
            Map<String, String> statistics = Collections.synchronizedMap(new LinkedHashMap<>());
            List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();

            ExecutorService executor = Executors.newCachedThreadPool();
            runAsync(() -> fillInStatistics(statistics, "Inlet Active Power", "/inlet/0/activePower", GET_READING), executor)
                    .thenRunAsync(() -> fillInStatistics(statistics, "Inlet RMS Current", "/inlet/0/current", GET_READING), executor)
                    .thenRunAsync(() -> fillInStatistics(statistics, "Inlet Line Frequency", "/inlet/0/lineFrequency", GET_READING), executor)
                    .thenRunAsync(() -> fillInStatistics(statistics, "Inlet RMS Voltage", "/inlet/0/voltage", GET_READING), executor)
                    .thenApplyAsync(ignore -> getOutletsCount(), executor)
                    .thenAcceptAsync(count -> IntStream.range(0, count)
                            .parallel()
                            .mapToObj(num -> runAsync(() -> fillInOutletState(statistics, advancedControllableProperties, num), executor)
                                    .thenRunAsync(() -> fillInStatistics(statistics, getOutletRMSName(num),
                                            "/outlet/" + num + "/current", GET_READING), executor)
                            )
                            .forEach(CompletableFuture::join), executor)
                    .get(30, TimeUnit.SECONDS);

            executor.shutdownNow();

            provisionTypedStatistics(statistics, extendedStatistics);
            extendedStatistics.setControllableProperties(advancedControllableProperties);
            localStatistics = extendedStatistics;
        } finally {
            reentrantLock.unlock();
        }
        return Collections.singletonList(extendedStatistics);
    }

    private void provisionTypedStatistics(Map<String, String> statistics, ExtendedStatistics extendedStatistics) {
        Map<String, String> dynamicStatistics = new HashMap<>();
        Map<String, String> staticStatistics = new HashMap<>();
        statistics.forEach((s, s2) -> {
            if (!historicalProperties.isEmpty() && historicalProperties.contains(s)
                    && DynamicStatisticsDefinitions.checkIfExists(s)) {
                dynamicStatistics.put(s, s2);
            } else {
                staticStatistics.put(s, s2);
            }
        });
        extendedStatistics.setDynamicStatistics(dynamicStatistics);
        extendedStatistics.setStatistics(staticStatistics);
    }

    /**
     * @return pingTimeout value if host is not reachable within
     * the pingTimeout, a ping time in milliseconds otherwise
     * if ping is 0ms it's rounded up to 1ms to avoid IU issues on Symphony portal
     *
     * @throws IOException
     */
    @Override
    public int ping() throws IOException {
        long pingResultTotal = 0L;

        for (int i = 0; i < this.getPingAttempts(); i++) {
            long startTime = System.currentTimeMillis();

            try (Socket puSocketConnection = new Socket(this.getHost(), this.getPort())) {
                puSocketConnection.setSoTimeout(this.getPingTimeout());

                if (puSocketConnection.isConnected()) {
                    long endTime = System.currentTimeMillis();
                    long pingResult = endTime - startTime;
                    pingResultTotal += pingResult;
                    if (this.logger.isTraceEnabled()) {
                        this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, this.getHost(), this.getPort(), pingResult));
                    }
                } else {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
                    }
                    return this.getPingTimeout();
                }
            } catch (SocketTimeoutException tex) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
                }
                return this.getPingTimeout();
            }
        }
        return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
    }

    private int getOutletsCount() {
        try {
            return doPostFiltered("model/outlet", null, "numberOfOutlets").asInt();
        } catch (Exception e) {
            throw new RuntimeException("Method doesn't not work at the URI " + BASE_URI + "/model/outlet", e);
        }
    }

    private JsonNode doPostFiltered(String url, Object data, String path) throws Exception {
        return doPost(url, data, JsonNode.class).findPath(path);
    }

    private void fillInOutletState(Map<String, String> statistics, List<AdvancedControllableProperty> control, int outletNumber) {
        try {
            JsonNode result = doPost(BASE_URI + "/outlet/" + outletNumber,
                    prepareRPCRequest("getState"), JsonNode.class);

            if (result.findPath("available").asBoolean()) {
                String outletDisplayName = getOutletDisplayName(outletNumber);
                int outletDisplayValue = result.findPath("powerState").asInt();
                statistics.put(outletDisplayName, String.valueOf(outletDisplayValue));
                control.add(createSwitch(getOutletDisplayName(outletNumber), outletDisplayValue));
            }
        } catch (Exception e) {
            throw new RuntimeException("Method doesn't not work at the URI " + BASE_URI + "/model/outlet", e);
        }
    }

    private String getOutletDisplayName(int outletNumber) {
        // in API used numeration from 0, but in UI from 1
        int displayOutletNumber = outletNumber + 1;
        return String.format("%s %d", OUTLET, displayOutletNumber);
    }

    private String getOutletRMSName(int outletNumber) {
        // in API used numeration from 0, but in UI from 1
        int displayOutletNumber = outletNumber + 1;
        return String.format("%s %d %s", OUTLET, displayOutletNumber, "RMS Current");
    }

    private void fillInStatistics(Map<String, String> statistics, String fieldName, String url, String method) {
        Optional.ofNullable(call(url, method)).ifPresent(response -> statistics.put(fieldName, response));
    }

    private String call(String url, String method) {
        try {
            return doPostFiltered(BASE_URI + url, prepareRPCRequest(method), "value").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * RPC request preparation, to avoid copy-pasting in code
     *
     * @param method method to use for an RPC request
     * @return Map with an rpcRequest, containing a method needed
     */
    private Map prepareRPCRequest(String method) {
        Map<String, String> rpcRequest = new HashMap();
        rpcRequest.put("jsonrpc", "2.0");
        rpcRequest.put("method", method);
        return rpcRequest;
    }

    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) {
        controllableProperties.forEach(this::controlProperty);
    }

    /**
     * Control action is performed. Currently supports switching outlets on/off.
     * Need to make sure control properties are not blocked by any REST API call
     * to avoid interfering and timeout exceptions.
     */
    @Override
    public void controlProperty(ControllableProperty controllableProperty) {
        reentrantLock.lock();
        try {
            controlsRunning = true;
            ExecutorService controlsExecutor = Executors.newCachedThreadPool();
            controlsExecutor.execute(() -> {
                try {
                    setOutletState(controllableProperty);
                } finally {
                    scheduledExecutor.shutdownNow();
                    scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
                    // 3000ms timeout for the controls, so if multiple controls
                    // are triggered - they will be stacked up together, without having
                    // a statistics call after each of them
                    scheduledExecutor.schedule(() -> {
                        controlsExecutor.shutdown();
                        controlsRunning = false;
                        }, 3000, TimeUnit.MILLISECONDS);
                }
            });
            if(localStatistics != null) {
                int controlValue = Integer.parseInt(String.valueOf(controllableProperty.getValue()));
                String propertyName = controllableProperty.getProperty();
                localStatistics.getControllableProperties().stream().filter(advancedControllableProperty ->
                        propertyName.equals(advancedControllableProperty.getName())).findFirst()
                        .ifPresent(advancedControllableProperty -> advancedControllableProperty.setValue(controlValue));
            }
        } finally {
            reentrantLock.unlock();
        }

    }

    /**
     * Sets outlet state to either on or off.
     * localStatistics variable is populated by the statistics/control status values on first statistics
     * collection cycle. Since the controls are being stacked together - it's possible that symphony tries to fetch statistics
     * while some of the controls actions are not finished yet. For this matter the latest "real" toggle values are
     * put into the localStatistics variable.
     */
    private void setOutletState(ControllableProperty controllableProperty) {
        String property = controllableProperty.getProperty();
        // in API used numeration from 0, but in UI from 1
        int outletNumber = Integer.parseInt(property.substring(OUTLET.length()).trim()) - 1;
        String uri = BASE_URI + "/outlet/" + outletNumber;

        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"setPowerState\",\"params\":{\"pstate\":" + controllableProperty.getValue() + "}}";
        try {
            int responseCode = doPostFiltered(uri, data, "_ret_").asInt();
            if (!this.logger.isWarnEnabled()) {
                return;
            }
            switch (responseCode) {
                case 0:
                    this.logger.info("SetPowerState method for Outlet " + outletNumber + " works, response is good");
                    break;
                case 1:
                    this.logger.warn("SetPowerState method for Outlet " + outletNumber + " error: OUTLET NOT SWITCHABLE");
                    break;
                case 2:
                    this.logger.warn("SetPowerState method for Outlet " + outletNumber + " error: LOAD SHEDDING ACTIVE");
                    break;
                case 3:
                    this.logger.warn("SetPowerState method for Outlet " + outletNumber + " error: OUTLET DISABLED");
                    break;
                case 4:
                    this.logger.warn("SetPowerState method for Outlet " + outletNumber + " error: OUTLET NOT OFF");
                    break;
                default:
                    this.logger.warn("Unknown responseCode " + responseCode + " is returned for SetPowerState method for Outlet " + outletNumber);
                    break;
            }
        } catch (Exception e) {
            if (this.logger.isErrorEnabled()) {
                this.logger.error("controlProperty property=" + controllableProperty.getProperty()
                        + "value=" + controllableProperty.getValue() +
                        " deviceId=" + controllableProperty.getDeviceId(), e);
            }

        }
    }
}