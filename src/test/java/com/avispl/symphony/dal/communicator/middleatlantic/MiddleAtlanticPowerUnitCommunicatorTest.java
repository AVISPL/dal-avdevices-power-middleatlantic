package com.avispl.symphony.dal.communicator.middleatlantic;

import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.dal.communicator.HttpCommunicator;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class MiddleAtlanticPowerUnitCommunicatorTest {
    static MiddleAtlanticPowerUnitCommunicator middleAtlanticPowerUnitCommunicator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().httpsPort(443)
            .basicAdminAuthenticator("admin", "admin").bindAddress("127.0.0.1"));

    @BeforeClass
    public static void init() throws Exception {
        middleAtlanticPowerUnitCommunicator = new MiddleAtlanticPowerUnitCommunicator();
        middleAtlanticPowerUnitCommunicator.setTrustAllCertificates(true);
        middleAtlanticPowerUnitCommunicator.setProtocol("https");
        middleAtlanticPowerUnitCommunicator.setContentType("application/json");
        middleAtlanticPowerUnitCommunicator.setPort(443);
        middleAtlanticPowerUnitCommunicator.setHost("127.0.0.1");
        middleAtlanticPowerUnitCommunicator.setAuthenticationScheme(HttpCommunicator.AuthenticationScheme.Basic);
        middleAtlanticPowerUnitCommunicator.setLogin("admin");
        middleAtlanticPowerUnitCommunicator.setPassword("admin");
        middleAtlanticPowerUnitCommunicator.init();
    }

    @Test
    public void powerUnitIsPingable() throws Exception {
        Assert.assertTrue(middleAtlanticPowerUnitCommunicator.ping() > -1);
        Assert.assertTrue(middleAtlanticPowerUnitCommunicator.ping() < 1000);
    }

//    @Test
    public void powerUnitThreadPoolIsRepeatedlyExecutable() {

    }

    @Test
    public void checkPowerUnitStatistics() throws Exception {
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
    }

    @Test
    public void testGetPowerUnitStatistics(){

    }

}
