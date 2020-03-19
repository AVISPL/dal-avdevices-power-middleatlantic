package com.avispl.symphony.dal.communicator.middleatlantic;

import com.atlassian.ta.wiremockpactgenerator.WireMockPactGenerator;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.dal.communicator.HttpCommunicator;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        Assert.assertTrue( pingResult < 1000 && pingResult > -1);
    }

    @Test
    public void verifyPowerUnitStatistics() throws Exception {
        List<Statistics> statisticsList = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();

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

        Assert.assertFalse(statisticsList.isEmpty());
        Assert.assertFalse(((ExtendedStatistics)statisticsList.get(0)).getStatistics().isEmpty());
    }

    @Test
    public void powerUnitThreadPoolIsRepeatedlyExecutable() throws Exception {
        List<Statistics> statisticsListFirstCall = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();
        List<Statistics> statisticsListSecondCall = middleAtlanticPowerUnitCommunicator.getMultipleStatistics();

        Assert.assertFalse(statisticsListFirstCall.isEmpty());
        Assert.assertFalse(((ExtendedStatistics)statisticsListFirstCall.get(0)).getStatistics().isEmpty());
        Assert.assertFalse(statisticsListSecondCall.isEmpty());
        Assert.assertFalse(((ExtendedStatistics)statisticsListSecondCall.get(0)).getStatistics().isEmpty());
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
