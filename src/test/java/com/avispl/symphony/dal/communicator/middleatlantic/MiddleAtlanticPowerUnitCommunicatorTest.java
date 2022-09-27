package com.avispl.symphony.dal.communicator.middleatlantic;

import com.atlassian.ta.wiremockpactgenerator.WireMockPactGenerator;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.dal.communicator.HttpCommunicator;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.Thread.sleep;

@Tag("test")
public class MiddleAtlanticPowerUnitCommunicatorTest {
    static MiddleAtlanticPowerUnitCommunicator middleAtlanticPowerUnitCommunicator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().dynamicHttpsPort()
            .basicAdminAuthenticator("admin", "admin").bindAddress("127.0.0.1"));

    {
        wireMockRule.addMockServiceRequestListener(WireMockPactGenerator
                .builder("middle-atlantic-power-unit-adapter", "middle-atlantic-power-unit")
                .withRequestHeaderWhitelist("authorization", "content-type").build());
        wireMockRule.start();
    }

    @BeforeEach
    public void init() throws Exception {
        middleAtlanticPowerUnitCommunicator = new MiddleAtlanticPowerUnitCommunicator();
        middleAtlanticPowerUnitCommunicator.setTrustAllCertificates(true);
        middleAtlanticPowerUnitCommunicator.setProtocol("https");
        middleAtlanticPowerUnitCommunicator.setContentType("application/json");
        middleAtlanticPowerUnitCommunicator.setPort(wireMockRule.httpsPort());
        middleAtlanticPowerUnitCommunicator.setHost("127.0.0.1");
        middleAtlanticPowerUnitCommunicator.setAuthenticationScheme(HttpCommunicator.AuthenticationScheme.Basic);
        middleAtlanticPowerUnitCommunicator.setLogin("admin");
        middleAtlanticPowerUnitCommunicator.setPassword("admin");
        middleAtlanticPowerUnitCommunicator.init();
    }

    @Test
    public void powerUnitIsPingable() throws Exception {
        int pingResult = middleAtlanticPowerUnitCommunicator.ping();
        Assertions.assertTrue(pingResult < 1000 && pingResult > -1);
    }

    @Test
    public void verifyPowerUnitStatistics() throws Exception {
        middleAtlanticPowerUnitCommunicator.setHistoricalProperties("Outlet 1 RMS Current, Outlet 2 RMS Current, Inlet Active Power, Outlet 1");
        List<Statistics> statisticsList = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();

        Assertions.assertFalse(statisticsList.isEmpty());

        ExtendedStatistics extendedStatistics = (ExtendedStatistics)middleAtlanticPowerUnitCommunicator.getMultipleStatistics().get(0);
        Map<String, String> statisticsMap = extendedStatistics.getStatistics();
        Map<String, String> dynamicStatisticsMap = extendedStatistics.getDynamicStatistics();

        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/activePower")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/lineFrequency")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/voltage")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/outlet")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/7")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/6")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/5")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/4")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/3")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/2")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/1")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/7/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/6/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/5/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/4/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/3/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/2/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/1/current")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0/current")));

        Assertions.assertNotNull(statisticsMap);
        Assertions.assertNotNull(dynamicStatisticsMap);
        Assertions.assertFalse(statisticsMap.isEmpty());
        Assertions.assertFalse(dynamicStatisticsMap.isEmpty());

        Assertions.assertEquals("59.999999", statisticsMap.get("Inlet Line Frequency"));
        Assertions.assertEquals("0.796", statisticsMap.get("Inlet RMS Current"));
        Assertions.assertEquals("118.829997", statisticsMap.get("Inlet RMS Voltage"));
        Assertions.assertEquals("0.496", statisticsMap.get("Outlet 3 RMS Current"));
        Assertions.assertEquals("74.460004", dynamicStatisticsMap.get("Inlet Active Power"));
        Assertions.assertEquals("0.496", dynamicStatisticsMap.get("Outlet 1 RMS Current"));
        Assertions.assertEquals("0.496", dynamicStatisticsMap.get("Outlet 2 RMS Current"));
        Assertions.assertNotNull(statisticsMap.get("Outlet 1"));
        Assertions.assertNull(dynamicStatisticsMap.get("Outlet 1"));
    }

    @Test
    public void powerUnitThreadPoolIsRepeatedlyExecutable() throws Exception {
        List<Statistics> statisticsListFirstCall = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();
        List<Statistics> statisticsListSecondCall = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();

        Assertions.assertFalse(statisticsListFirstCall.isEmpty());
        Assertions.assertFalse(((ExtendedStatistics)statisticsListFirstCall.get(0)).getStatistics().isEmpty());
        Assertions.assertFalse(statisticsListSecondCall.isEmpty());
        Assertions.assertFalse(((ExtendedStatistics)statisticsListSecondCall.get(0)).getStatistics().isEmpty());
    }

    @Test
    public void verifyPowerStateControlON() throws InterruptedException {
        ControllableProperty controllablePropertyPowerStateON = new ControllableProperty();
        controllablePropertyPowerStateON.setDeviceId("deviceId");
        controllablePropertyPowerStateON.setProperty("Outlet 1");
        controllablePropertyPowerStateON.setValue(1);

       middleAtlanticPowerUnitCommunicator.controlProperty(controllablePropertyPowerStateON);
       sleep(1000);
       wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0")));
    }

    @Test
    public void verifyPowerStateControlOFF() throws InterruptedException {
        ControllableProperty controllablePropertyPowerStateOFF = new ControllableProperty();
        controllablePropertyPowerStateOFF.setDeviceId("deviceId");
        controllablePropertyPowerStateOFF.setProperty("Outlet 1");
        controllablePropertyPowerStateOFF.setValue(0);

        middleAtlanticPowerUnitCommunicator.controlProperty(controllablePropertyPowerStateOFF);
        sleep(1000);
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0")));
    }
}
