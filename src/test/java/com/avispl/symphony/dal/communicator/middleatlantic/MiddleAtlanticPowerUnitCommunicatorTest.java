package com.avispl.symphony.dal.communicator.middleatlantic;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.dal.communicator.HttpCommunicator;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class MiddleAtlanticPowerUnitCommunicatorTest {
    static MiddleAtlanticPowerUnitCommunicator middleAtlanticPowerUnitCommunicator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().dynamicHttpsPort()
            .basicAdminAuthenticator("admin", "admin").bindAddress("127.0.0.1"));

    @Before
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

        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/activePower")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/lineFrequency")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/inlet/0/voltage")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/outlet")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/7")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/6")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/5")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/4")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/3")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/2")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/1")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/7/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/6/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/5/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/4/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/3/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/2/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/1/current")).withBasicAuth(new BasicCredentials("admin", "admin")));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/model/pdu/0/outlet/0/current")).withBasicAuth(new BasicCredentials("admin", "admin")));

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
}
