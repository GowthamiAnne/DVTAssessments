package com.airline.loyalty;

import com.airline.loyalty.model.PointsQuoteRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.ServerSocket;
import java.io.IOException;
import java.time.LocalDate;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

import static io.restassured.RestAssured.given;

@ExtendWith(VertxExtension.class)
public abstract class BaseComponentTest {
    protected WireMockServer fxServiceMock;
    protected WireMockServer promoServiceMock;
    protected int appPort;
    protected String baseUrl;
    

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) throws IOException {
        // Find random available ports
        appPort = findRandomPort();
        int fxPort = findRandomPort();
        int promoPort = findRandomPort();
        int metricsPort = findRandomPort();

        baseUrl = "http://localhost:" + appPort;

        // Start WireMock servers
        fxServiceMock = new WireMockServer(WireMockConfiguration.options().port(fxPort));
        fxServiceMock.start();

        promoServiceMock = new WireMockServer(WireMockConfiguration.options().port(promoPort));
        promoServiceMock.start();

        // Deploy verticle with test configuration
        JsonObject config = new JsonObject()
            .put("http", new JsonObject()
                .put("port", appPort)
                .put("host", "localhost"))
            .put("fxService", new JsonObject()
                .put("host", "localhost")
                .put("port", fxPort)
                .put("ssl", false)
                .put("timeout", 3000)
                .put("retries", 2)
                .put("path", "/v1/rates"))
            .put("promoService", new JsonObject()
                .put("host", "localhost")
                .put("port", promoPort)
                .put("ssl", false)
                .put("timeout", 2000)
                .put("path", "/v1/promos"))
            .put("business", new JsonObject()
                .put("maxPoints", 50000)
                .put("expiryWarningDays", 7)
                .put("tierMultipliers", new JsonObject()
                    .put("NONE", 0.0)
                    .put("SILVER", 0.15)
                    .put("GOLD", 0.30)
                    .put("PLATINUM", 0.50)))
            .put("observability", new JsonObject()
                .put("metricsEnabled", true)
                .put("metricsPort", metricsPort));
        System.out.print("FX Service"+fxPort);
        System.out.print("PROMO Service"+promoPort);
        DeploymentOptions options = new DeploymentOptions().setConfig(config);
        
        vertx.deployVerticle(new MainVerticle(), options)
            .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        fxServiceMock.stop();
        promoServiceMock.stop();
        vertx.close().onComplete(testContext.succeedingThenComplete());
    }

    private int findRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
    
    public void stubFxRate(String from, String to, double rate) {
        fxServiceMock.stubFor(get(urlPathEqualTo("/v1/rates"))
            .withQueryParam("from", equalTo(from))
            .withQueryParam("to", equalTo(to))
            .willReturn(okJson("""
                    {"fromCurrency":"%s","toCurrency":"%s","rate":%f,"timestamp":"%s"}
                    """.formatted(from, to, rate, LocalDate.now().toString()))));
    }
	
	public void stubPromo(String promoCode, double multiplier, LocalDate expiry, boolean active) {
        promoServiceMock.stubFor(get(urlPathEqualTo("/v1/promos/%s".formatted(promoCode)))
            .willReturn(okJson("""
                    {
                        "promoCode": "%s",
                        "bonusMultiplier": %f,
                        "expiryDate": "%s",
                        "active": %s
                    }
                    """.formatted(promoCode, multiplier, expiry.toString(), active))));
    }

	public ValidatableResponse postQuote(PointsQuoteRequest request) {
	    return given()
	            .baseUri(baseUrl)
	            .contentType(ContentType.JSON)
	            .body(request)
	            .when()
	            .post("/v1/points/quote")
	            .then();
	}

}
