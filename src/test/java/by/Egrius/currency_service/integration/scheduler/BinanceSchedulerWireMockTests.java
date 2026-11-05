package by.Egrius.currency_service.integration.scheduler;

import by.Egrius.currency_service.CurrencyServiceApplication;
import by.Egrius.currency_service.dto.response.BinanceAverageResponse;
import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.integration.config.BaseIntegrationTest;
import by.Egrius.currency_service.repository.PriceRepository;
import by.Egrius.currency_service.service.BinanceScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CurrencyServiceApplication.class)
public class BinanceSchedulerWireMockTests extends BaseIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("binance.api.url", wireMock::baseUrl);
        registry.add("binance.poll-interval", () -> "PT1S");
        registry.add("binance.initial-delay", () -> "PT3S");
    }

    @MockitoSpyBean
    private BinanceScheduler binanceScheduler;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${binance.api.avg-price-url}")
    private String avgPriceEndpointUrl;

    @Value("${binance.symbol}")
    private String binanceSymbol;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);
        Mockito.reset(binanceScheduler);
        priceRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    void shouldFetchAndSavePrice_whenBinanceReturns200() throws JsonProcessingException, InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        assertEquals(0L, priceRepository.count());

        BinanceAverageResponse mockResponse = new BinanceAverageResponse(
                1, new BigDecimal("200.12300000"), 123456789L);

        wireMock.stubFor(get(urlPathEqualTo(avgPriceEndpointUrl))
                .withQueryParam("symbol", equalTo(binanceSymbol))
                .willReturn(okJson(objectMapper.writeValueAsString(mockResponse)))
        );

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MIN);

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();

        assertTrue(latch.await(15, TimeUnit.SECONDS));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);

        Price price = priceRepository.findLastSavedPriceByFetchedAt().orElseThrow();

        assertEquals(0, mockResponse.price().compareTo(price.getPrice()));

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(avgPriceEndpointUrl)));

    }

    @Test
    void shouldEnterBackoffAndSkipNextTicks_whenBinanceReturns429WithRetryAfter() throws InterruptedException {

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(2);

        assertEquals(0L, priceRepository.count());

        wireMock.stubFor(get(urlPathEqualTo(avgPriceEndpointUrl))
                .withQueryParam("symbol", equalTo(binanceSymbol))
                .willReturn(responseDefinition()
                        .withStatus(429)
                        .withHeader("Retry-After", "60")
                ));

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch1.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MIN);

        assertTrue(latch1.await(15, TimeUnit.SECONDS));

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch2.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();


        assertTrue(latch2.await(15, TimeUnit.SECONDS));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);

        assertEquals(0L, priceRepository.count());

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(avgPriceEndpointUrl)));
    }

    @Test
    void shouldNotCrashOn500_andRecoverOnNextTickWhenBinanceIsBack() throws Exception {

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        assertEquals(0L, priceRepository.count());

        wireMock.stubFor(get(urlPathEqualTo(avgPriceEndpointUrl))
                .withQueryParam("symbol", equalTo(binanceSymbol))
                .willReturn(responseDefinition()
                        .withStatus(500)
                ));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MIN);

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch1.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();

        // Здесь должен словить 500 и обработать
        assertTrue(latch1.await(15, TimeUnit.SECONDS));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);

        assertEquals(0L, priceRepository.count());

        // Подготовка ко второму запросу
        BinanceAverageResponse mockResponse = new BinanceAverageResponse(
                1, new BigDecimal("200.12300000"), 123456789L);

        wireMock.stubFor(get(urlPathEqualTo(avgPriceEndpointUrl))
                .withQueryParam("symbol", equalTo(binanceSymbol))
                .willReturn(okJson(objectMapper.writeValueAsString(mockResponse)))
        );

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MIN);

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch2.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();

        assertTrue(latch2.await(15, TimeUnit.SECONDS));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);

        Price price = priceRepository.findLastSavedPriceByFetchedAt().orElseThrow();

        assertEquals(0, mockResponse.price().compareTo(price.getPrice()));

        wireMock.verify(2, getRequestedFor(urlPathEqualTo(avgPriceEndpointUrl)));
    }

    @Test
    void shouldNotSaveRecord_whenBinanceReturnsEmptyOrMalformedBody() throws InterruptedException {

        assertEquals(0L, priceRepository.count());

        CountDownLatch latch1 = new CountDownLatch(1);

        wireMock.stubFor(get(urlPathEqualTo(avgPriceEndpointUrl))
                .withQueryParam("symbol", equalTo(binanceSymbol))
                .willReturn(okJson("{}"))
        );

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MIN);

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch1.countDown();

            return result;
        }).
                when(binanceScheduler).requestAvgPriceFromApi();

        assertTrue(latch1.await(15, TimeUnit.SECONDS));

        ReflectionTestUtils.setField(binanceScheduler, "backOffUntil", Instant.MAX);

        assertEquals(0L, priceRepository.count());

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(avgPriceEndpointUrl)));
    }
}