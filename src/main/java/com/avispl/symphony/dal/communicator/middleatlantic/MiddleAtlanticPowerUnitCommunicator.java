package com.avispl.symphony.dal.communicator.middleatlantic;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;

public class MiddleAtlanticPowerUnitCommunicator extends RestCommunicator implements Monitorable, Controller {

    private Log log = LogFactory.getLog(this.getClass());
    private final String BACE_URI = "model/pdu/0/";
    private final String OUTLET = "Outlet ";
    private final String GET_READING = "getReading";
    private CountDownLatch latch;
    private ExecutorService executor;

    /**
     * MiddleAtlanticPowerUnit constructor.
     */
    public MiddleAtlanticPowerUnitCommunicator() {
        super();
        // override default value to trust all certificates - power unit typically do not have trusted certificates installed
        // it can be changed back by configuration
        setTrustAllCertificates(true);
    }

    // FOR LOCAL TESTING
    public static void main(String[] args) throws Exception {
        MiddleAtlanticPowerUnitCommunicator device = new MiddleAtlanticPowerUnitCommunicator();
        device.setTrustAllCertificates(true);
        device.setProtocol("https");
        device.setContentType("application/json");
        device.setPort(443);
        device.setHost("172.31.254.201");
        device.setAuthenticationScheme(AuthenticationScheme.Basic);
        device.setLogin("admin");
        device.setPassword("admin");
        device.init();

        long startTime = System.currentTimeMillis();
        device.getMultipleStatistics();
        System.out.println("Time = " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    protected void authenticate() throws Exception {
        // nothing to do here, authentication is done in individual requests
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        Map<String, String> statistics = Collections.synchronizedMap(new LinkedHashMap<>());
        Map<String, String> control = new ConcurrentHashMap<>();

        int outletsCount = getOutletsCount();
        executor = Executors.newFixedThreadPool(outletsCount * 4 + 8);
        latch = new CountDownLatch(outletsCount * 2 + 4);
        fillInStatistics(statistics, "Inlet Active Power", "/inlet/0/activePower", GET_READING);
        fillInStatistics(statistics, "Inlet RMS Current", "/inlet/0/current", GET_READING);
        fillInStatistics(statistics, "Inlet Line Frequency", "/inlet/0/lineFrequency", GET_READING);
        fillInStatistics(statistics, "Inlet RMS Voltage", "/inlet/0/voltage", GET_READING);

        for (int outletNumber = 0; outletNumber < outletsCount; ++outletNumber) {
            fillInOutletState(statistics, control, outletNumber);
            fillInStatistics(statistics, OUTLET + (outletNumber + 1) + " RMS Current",
                    "outlet/" + outletNumber + "/current", GET_READING);
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        extendedStatistics.setStatistics(statistics);
        extendedStatistics.setControl(control);

        return new ArrayList<>(Arrays.asList(extendedStatistics));
    }

    private int getOutletsCount() {
        try {
            Map result = doPost("model/outlet", null, Map.class);
            return (int) ((Map) ((Map) result.get("result")).get("_ret_")).get("numberOfOutlets");
        } catch (Exception e) {
            throw new RuntimeException("Method doesn't not work at the URI " + BACE_URI + "model/outlet", e);
        }
    }

    private void fillInOutletState(Map<String, String> statistics, Map<String, String> control, int outletNumber) {
        CompletableFuture.runAsync(() -> {
            try {
                int outletNumberUI = outletNumber + 1;
                Map result = doPost(BACE_URI + "/outlet/" + outletNumber,
                        ImmutableMap.of(
                                "jsonrpc", "2.0",
                                "method", "getState"), Map.class);
                if ((boolean) ((Map) ((Map) result.get("result")).get("_ret_")).get("available")) {
                    latch.countDown();
                    statistics.put(OUTLET + outletNumberUI,
                            String.valueOf((int) ((Map) ((Map) result.get("result")).get("_ret_")).get("powerState") == 1));
                    control.put(OUTLET + outletNumberUI, "Toggle");
                }
            } catch (Exception e) {
                throw new RuntimeException("Method doesn't not work at the URI " + BACE_URI + "model/outlet", e);
            }
        }, executor);
    }

    private void fillInStatistics(Map<String, String> statistics, String fieldName, String url, String method) {
        CompletableFuture.runAsync(() -> {
            String response = call(url, method);
            if (response != null) {
                statistics.put(fieldName, response);
            }
            latch.countDown();
        }, executor);
    }

    private String call(String url, String method) {
        try {
            return (((Map) ((Map) doPost(BACE_URI + url,
                    ImmutableMap.of(
                            "jsonrpc", "2.0",
                            "method", method),
                    Map.class)
                    .get("result"))
                    .get("_ret_"))
                    .get("value").toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
        if (CollectionUtils.isEmpty(controllableProperties)) {
            throw new IllegalArgumentException("Controllable properties cannot be null or empty");
        }

        for (ControllableProperty controllableProperty : controllableProperties) {
            controlProperty(controllableProperty);
        }
    }

    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        CompletableFuture.runAsync(() -> {
            try {
                setOutletState(controllableProperty);
            } catch (Exception e) {
                log.error("controlProperty property=" + controllableProperty.getProperty()
                        + "value=" + controllableProperty.getValue() +
                        " deviceId=" + controllableProperty.getDeviceId(), e);
            }
        });
    }

    private void setOutletState(ControllableProperty controllableProperty) throws Exception {
        String response;
        String property = controllableProperty.getProperty();
        int outletNumber = Integer.parseInt(String.valueOf(property.charAt(property.length() - 1)));
        String uri = BACE_URI + "outlet/" + (outletNumber - 1);
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"setPowerState\",\"params\":{\"pstate\":" + controllableProperty.getValue() + "}}";
        try {
            Map map = doPost(uri, data, Map.class);
            log.info(map.toString());

            response = ((Map) doPost(uri, data, Map.class)
                    .get("result"))
                    .get("_ret_")
                    .toString();
        } catch (Exception e) {
            throw new Exception("SetPowerState method doesn't not work at the URI " + uri, e);
        }
        int responseCode = Integer.parseInt(response);
        switch (responseCode) {
            case 0:
                log.info("SetPowerState method for Outlet " + outletNumber + " is works, responce is good");
                break;
            case 1:
                log.info("SetPowerState method for Outlet " + outletNumber + " error: OUTLET NOT SWITCHABLE");
                break;
            case 2:
                log.info("SetPowerState method for Outlet " + outletNumber + " error: LOAD SHEDDING ACTIVE");
                break;
            case 3:
                log.info("SetPowerState method for Outlet " + outletNumber + " error: OUTLET DISABLED");
                break;
            case 4:
                log.info("SetPowerState method for Outlet " + outletNumber + " error: OUTLET NOT OFF");
                break;
        }
    }
}