package by.Egrius.currency_service.integration.service;

import by.Egrius.currency_service.CurrencyServiceApplication;
import by.Egrius.currency_service.dto.price.AveragePriceReadDto;
import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.dto.response.BinanceAverageResponse;
import by.Egrius.currency_service.dto.response.PagingPricesResponse;
import by.Egrius.currency_service.dto.response.TrendResponse;
import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.entity.Trend;
import by.Egrius.currency_service.exception.PriceNotFoundException;
import by.Egrius.currency_service.integration.config.BaseIntegrationTest;
import by.Egrius.currency_service.repository.PriceRepository;
import by.Egrius.currency_service.service.BinanceScheduler;
import by.Egrius.currency_service.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CurrencyServiceApplication.class)
public class PriceServiceIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private PriceService priceService;

    @Autowired
    private PriceRepository priceRepository;

    @MockitoBean
    private BinanceScheduler binanceScheduler;

    @Value("${binance.symbol}")
    private String binanceSymbol;

    private final Instant t1 = Instant.parse("2026-01-01T01:00:00Z");
    private final Instant t2 = t1.plus(1, ChronoUnit.HOURS);
    private final Instant t3 = t2.plus(1, ChronoUnit.HOURS);

    @BeforeEach
    void cleanUp() {
        priceRepository.deleteAll();
    }

    @Nested
    class CreateAndGetLastSavedTests {

        @Test
        void shouldSaveFromBinanceResponse_andReturnAsLastSaved() {
            BinanceAverageResponse olderResponse = new BinanceAverageResponse(
                    5, new BigDecimal("500.00"), t1.toEpochMilli()
            );
            BinanceAverageResponse newerResponse = new BinanceAverageResponse(
                    5, new BigDecimal("550.25"), t2.toEpochMilli()
            );

            PriceReadDto createdOlder = priceService.createFromBinanceResponse(olderResponse, t1);
            PriceReadDto createdNewer = priceService.createFromBinanceResponse(newerResponse, t2);

            assertNotNull(createdOlder);
            assertNotNull(createdNewer);

            PriceReadDto lastSaved = priceService.getLastSavedPrice();

            assertEquals(binanceSymbol, lastSaved.symbol());
            assertEquals(t2, lastSaved.fetchedAt());
            assertEquals(0, new BigDecimal("550.25").compareTo(lastSaved.price()));
        }

        @Test
        void shouldThrowPriceNotFoundException_whenDbIsEmpty() {
            assertThrows(PriceNotFoundException.class, () -> priceService.getLastSavedPrice());
        }
    }

    @Nested
    class GetAveragePriceTests {

        @Test
        void shouldCalculateAveragePriceFromDb() {
            Price p1 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(t1).build();
            Price p2 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("200.00")).fetchedAt(t2).build();
            Price p3 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("900.00")).fetchedAt(t3).build();

            priceRepository.saveAll(List.of(p1, p2, p3));

            AveragePriceReadDto averageDto = priceService.getAveragePrice(t1, t2);

            assertNotNull(averageDto);
            assertEquals(0, new BigDecimal("150.00").compareTo(averageDto.averagePrice()));
        }

        @Test
        void shouldThrowPriceNotFoundException_whenNoRecordsInRange() {
            Price p1 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(t1).build();
            priceRepository.save(p1);

            Instant emptyRangeFrom = t2;
            Instant emptyRangeTo = t3;

            assertThrows(PriceNotFoundException.class,
                    () -> priceService.getAveragePrice(emptyRangeFrom, emptyRangeTo));
        }
    }

    @Nested
    class GetPagingPricesTests {

        @Test
        void shouldReturnMappedPageWithCorrectMetadata() {
            Price p1 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(t1).build();
            Price p2 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("200.00")).fetchedAt(t2).build();
            Price p3 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("300.00")).fetchedAt(t3).build();

            priceRepository.saveAll(List.of(p1, p2, p3));

            PagingPricesResponse<PriceReadDto> response = priceService.getPagingPricesByRange(t1, t3, 0, 2);

            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            assertEquals(0, response.getPage());
            assertEquals(2, response.getSize());
            assertEquals(3L, response.getTotalElements());
            assertTrue(response.isHasNext());

            PriceReadDto firstItem = response.getItems().get(0);
            assertEquals(binanceSymbol, firstItem.symbol());
            assertEquals(t3, firstItem.fetchedAt());
            assertEquals(0, new BigDecimal("300.00").compareTo(firstItem.price()));

            PriceReadDto secondItem = response.getItems().get(1);
            assertEquals(binanceSymbol, secondItem.symbol());
            assertEquals(t2, secondItem.fetchedAt());
            assertEquals(0, new BigDecimal("200.00").compareTo(secondItem.price()));
        }
    }

    @Nested
    class GetTrendByRangeTests {

        @Test
        void shouldReturnUpwardTrend_whenPriceRaised() {

            Instant from = t1;
            Instant to = t1.plus(1, ChronoUnit.DAYS);

            priceRepository.saveAll(List.of(
                Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(from).build(),
                Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.001")).fetchedAt(from.plus(1, ChronoUnit.HOURS)).build(),

                Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(to.minus(1, ChronoUnit.HOURS)).build(),
                Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.01")).fetchedAt(to).build()
            ));

            TrendResponse response = priceService.getTrendByRange(from, to);

            assertNotNull(response);
            assertEquals(Trend.UPWARD, response.trend());
            assertEquals(from, response.startDate());
            assertEquals(to, response.endDate());
        }

        @Test
        void shouldReturnDownwardTrend_whenPriceDecreased() {

            Instant from = t1;
            Instant to = t1.plus(1, ChronoUnit.DAYS);

            priceRepository.saveAll(List.of(
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.00")).fetchedAt(from).build(),

                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("109.988")).fetchedAt(to).build()
            ));

            TrendResponse response = priceService.getTrendByRange(from, to);

            assertNotNull(response);
            assertEquals(Trend.DOWNWARD, response.trend());
            assertEquals(from, response.startDate());
            assertEquals(to, response.endDate());
        }


        @Test
        void shouldReturnFlatTrend_whenPriceIsNotChanged() {

            Instant from = t1;
            Instant to = t1.plus(1, ChronoUnit.DAYS);

            priceRepository.saveAll(List.of(
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.00")).fetchedAt(from).build(),
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("112.00")).fetchedAt(from.plus(5, ChronoUnit.HOURS)).build(),

                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("112.00")).fetchedAt(to.minus(5, ChronoUnit.HOURS)).build(),
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.00")).fetchedAt(to).build()
            ));

            TrendResponse response = priceService.getTrendByRange(from, to);

            assertNotNull(response);
            assertEquals(Trend.FLAT, response.trend());
            assertEquals(from, response.startDate());
            assertEquals(to, response.endDate());
        }

        @Test
        void shouldRecalculateTrend_whenAllRecordsLocatedInOneBucket() {

            Instant from = t1;
            Instant to = t1.plus(10, ChronoUnit.HOURS);

            priceRepository.saveAll(List.of(
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.00")).fetchedAt(from.plus(1, ChronoUnit.MINUTES)).build(),

                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.011")).fetchedAt(from.plus(5, ChronoUnit.MINUTES)).build()
            ));

            TrendResponse response = priceService.getTrendByRange(from, to);

            assertNotNull(response);
            assertEquals(Trend.UPWARD, response.trend());
            assertEquals(from, response.startDate());
            assertEquals(to, response.endDate());

        }

        @Test
        void shouldReturnFlatAndNotCrashPostgres_whenOnlyOneRecordExistsInRange() {

            priceRepository.saveAll(List.of(
                    Price.builder().symbol(binanceSymbol).price(new BigDecimal("110.00")).fetchedAt(t1).build()
            ));

            TrendResponse response = priceService.getTrendByRange(t1, t1.plus(1, ChronoUnit.HOURS));

            assertNotNull(response);
            assertEquals(Trend.FLAT, response.trend());
        }
    }
}
