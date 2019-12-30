package com.avispl.symphony.dal.communicator.middleatlantic;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Provider("middle-atlantic-power-unit")
@PactFolder("pacts/")
public class MiddleAtlanticPowerUnitContractProviderTest {
    private static final int POWER_UNIT_PORT = 443;
    private static final String POWER_UNIT_IP_ADDRESS = "172.31.254.201";
    private static final String LOCAL_IP_ADDRESS = "127.0.0.1";
    private static final String POWER_UNIT_TEST_USERNAME = "admin";
    private static final String POWER_UNIT_TEST_PASSWORD = "admin";

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setup(){
        wireMockServer = new WireMockServer(options().dynamicPort().dynamicHttpsPort()
                .basicAdminAuthenticator(POWER_UNIT_TEST_USERNAME, POWER_UNIT_TEST_PASSWORD).bindAddress(LOCAL_IP_ADDRESS));
        wireMockServer.start();
    }

    @BeforeEach
    public void setTarget(PactVerificationContext context) {
        HttpsTestTarget target = new HttpsTestTarget(POWER_UNIT_IP_ADDRESS, POWER_UNIT_PORT, "/", true);
        context.setTarget(target);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, HttpRequest request, PactVerificationContext context) {
        context.verifyInteraction();
    }
}
