package by.Egrius.currency_service.integration.repository;

import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.integration.config.BaseIntegrationTest;
import by.Egrius.currency_service.repository.PriceRepository;
import by.Egrius.currency_service.repository.projection.TrendBucketProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class PriceRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private PriceRepository priceRepository;

    @Value("${binance.symbol}")
    private String binanceSymbol;

    @PersistenceContext
    private EntityManager entityManager;

    private final Instant price1FetchedAt = Instant.parse("2026-01-01T01:00:00Z");
    private final Instant price2FetchedAt = price1FetchedAt.plus(1, ChronoUnit.HOURS);
    private final Instant price3FetchedAt = price2FetchedAt.plus(1, ChronoUnit.HOURS);

    void setUp() {
        Price price1 = Price.builder().symbol(binanceSymbol).price(BigDecimal.ONE).fetchedAt(price1FetchedAt).build();
        Price price2 = Price.builder().symbol(binanceSymbol).price(BigDecimal.ONE).fetchedAt(price2FetchedAt).build();
        Price price3 = Price.builder().symbol(binanceSymbol).price(BigDecimal.ONE).fetchedAt(price3FetchedAt).build();

        priceRepository.saveAll(List.of(price1, price2, price3));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldThrowOnDuplicateSave() {
        Price price = Price.builder().symbol(binanceSymbol).price(BigDecimal.ONE).fetchedAt(price1FetchedAt).build();
        Price duplicate = Price.builder().symbol(binanceSymbol).price(BigDecimal.TEN).fetchedAt(price1FetchedAt).build();

        priceRepository.save(price);
        entityManager.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            priceRepository.save(duplicate);
            entityManager.flush();
        });
    }

    @Nested
    class LastSavedTests {

        @Test
        void shouldReturnLastSavedPrice_whenRepositoryNotEmpty() {
            setUp();

            Price lastSaved = priceRepository.findLastSavedPriceByFetchedAt()
                    .orElseThrow();

            assertEquals(price3FetchedAt, lastSaved.getFetchedAt());
        }

        @Test
        void shouldReturnEmptyOptional_whenRepositoryEmpty() {

            Optional<Price> lastSaved = priceRepository.findLastSavedPriceByFetchedAt();

            assertTrue(lastSaved.isEmpty());
        }
    }

    @Nested
    class ByRangePageableTests {

        @Test
        void shouldReturnCorrectDataWithOrdering() {
            setUp();

            Pageable pageable = PageRequest.of(0, 5);

            Page<Price> page = priceRepository.findPricesByRangePageable(price2FetchedAt, price3FetchedAt, pageable);

            assertTrue(page.hasContent());
            assertEquals(5, page.getSize());
            assertEquals(2, page.getTotalElements());
            assertFalse(page.hasNext());

            // В репозитории прописывал DESC, динамические упорядочивания не применял

            assertEquals(price3FetchedAt, page.getContent().get(0).getFetchedAt());
            assertEquals(price2FetchedAt, page.getContent().get(1).getFetchedAt());
        }

        @Test
        void shouldReturnEmptyPage_whenRepositoryEmpty() {

            Pageable pageable = PageRequest.of(0, 5);

            Page<Price> page = priceRepository.findPricesByRangePageable(price2FetchedAt, price3FetchedAt, pageable);

            assertFalse(page.hasContent());
            assertEquals(5, page.getSize());
            assertEquals(0, page.getTotalElements());
            assertFalse(page.hasNext());

        }
    }

    @Nested
    class AveragePriceTests {

        @Test
        void shouldReturnCorrectAveragePrice_whenRecordsInRange() {
            Price p1 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(price1FetchedAt).build();
            Price p2 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("200.00")).fetchedAt(price2FetchedAt).build();
            Price p3 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("999.00")).fetchedAt(price3FetchedAt).build();

            priceRepository.saveAll(List.of(p1, p2, p3));
            entityManager.flush();
            entityManager.clear();

            Optional<BigDecimal> avg = priceRepository.findAveragePriceByRange(price1FetchedAt, price2FetchedAt);

            assertTrue(avg.isPresent());
            assertEquals(0, new BigDecimal("150.00").compareTo(avg.get()));
        }

        @Test
        void shouldReturnEmptyOptional_whenNoRecordsInRange() {
            setUp();

            Instant from = price3FetchedAt.plus(1, ChronoUnit.HOURS);
            Instant to = from.plus(1, ChronoUnit.HOURS);

            Optional<BigDecimal> avg = priceRepository.findAveragePriceByRange(from, to);

            assertTrue(avg.isEmpty());
        }
    }

    @Nested
    class FindTrendByRangeTests {

        @Test
        void shouldReturnCorrectBuckets() {

            Instant from = Instant.parse("2026-01-01T01:00:00Z");
            int i;

            for (i = 0; i < 20; i++) {
                Price price = Price.builder().symbol(binanceSymbol).price(BigDecimal.ONE).fetchedAt(from.plus(i, ChronoUnit.HOURS)).build();
                priceRepository.save(price);
            }

            entityManager.flush();
            entityManager.clear();

            Instant to = from.plus(i, ChronoUnit.HOURS);

            int bucketCount = 3;

            List<TrendBucketProjection> buckets = priceRepository.findTrendByRange(from, to, bucketCount);

            assertFalse(buckets.isEmpty());
            assertEquals(bucketCount, buckets.size());
            assertEquals(bucketCount, buckets.getLast().getBucketNumber());
            assertEquals(1, buckets.getFirst().getBucketNumber());

        }

        @Test
        void shouldNotExcludeBoundary_andCalculateCorrectAverage() {
            // диапазон 2 часа, разбивка на 2 бакета
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T02:00:00Z");

            // бакет 1 [00:00, 01:00)
            Instant b1Min = from; // Ровно на нижней границе
            Instant b1Max = from.plus(30, ChronoUnit.MINUTES);
            Price p1 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("100.00")).fetchedAt(b1Min).build();
            Price p2 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("200.00")).fetchedAt(b1Max).build();

            // бакет 2 [01:00, 02:00]
            Instant b2Min = from.plus(60, ChronoUnit.MINUTES);
            Instant b2Max = to;
            Price p3 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("300.00")).fetchedAt(b2Min).build();
            Price p4 = Price.builder().symbol(binanceSymbol).price(new BigDecimal("500.00")).fetchedAt(b2Max).build();

            // Записи, которые за пределами [from, to], не должны быть в результате
            Price beforeBound = Price.builder().symbol(binanceSymbol).price(new BigDecimal("9999.00")).fetchedAt(from.minusSeconds(10)).build();
            Price afterBound = Price.builder().symbol(binanceSymbol).price(new BigDecimal("9999.00")).fetchedAt(to.plusSeconds(10)).build();

            priceRepository.saveAll(List.of(p1, p2, p3, p4, beforeBound, afterBound));
            entityManager.flush();
            entityManager.clear();

            List<TrendBucketProjection> buckets = priceRepository.findTrendByRange(from, to, 2);

            assertEquals(2, buckets.size());

            TrendBucketProjection first = buckets.get(0);

            log.info("First average: {}", first.getAveragePrice());
            log.info("First minFetchedAt: {}", first.getMinFetchedAt());
            log.info("First maxFetchedAt: {}", first.getMaxFetchedAt());

            assertEquals(1, first.getBucketNumber());
            assertEquals(0, new BigDecimal("150.00").compareTo(first.getAveragePrice()));
            assertEquals(b1Min, first.getMinFetchedAt());
            assertEquals(b1Max, first.getMaxFetchedAt());

            TrendBucketProjection second = buckets.get(1);

            log.info("Second average: {}", second.getAveragePrice());
            log.info("Second minFetchedAt: {}", second.getMinFetchedAt());
            log.info("Second maxFetchedAt: {}", second.getMaxFetchedAt());

            assertEquals(2, second.getBucketNumber());
            assertEquals(0, new BigDecimal("400.00").compareTo(second.getAveragePrice()));
            assertEquals(b2Min, second.getMinFetchedAt());
            assertEquals(b2Max, second.getMaxFetchedAt());
        }

        @Test
        void shouldSkipEmptyMiddleBuckets_andKeepAscendingOrder() {
            // 5 часов, 5 бакетов по 1 часу
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T05:00:00Z");


            // cтавим только в начало и в конец, т.е есть пустые бакеты между двумя записями
            Price firstHourPrice = Price.builder()
                    .symbol(binanceSymbol)
                    .price(new BigDecimal("10.00"))
                    .fetchedAt(from)
                    .build();

            Price lastHourPrice = Price.builder()
                    .symbol(binanceSymbol)
                    .price(new BigDecimal("20.00"))
                    .fetchedAt(to)
                    .build();

            priceRepository.saveAll(List.of(lastHourPrice, firstHourPrice));
            entityManager.flush();
            entityManager.clear();

            List<TrendBucketProjection> buckets = priceRepository.findTrendByRange(from, to, 5);

            assertEquals(2, buckets.size());
            assertEquals(1, buckets.get(0).getBucketNumber());
            assertEquals(5, buckets.get(1).getBucketNumber());
            assertEquals(0, new BigDecimal("10.00").compareTo(buckets.get(0).getAveragePrice()));
            assertEquals(0, new BigDecimal("20.00").compareTo(buckets.get(1).getAveragePrice()));
        }

        @Test
        void shouldReturnEmptyList_whenNoRecordsInRange() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            List<TrendBucketProjection> buckets = priceRepository.findTrendByRange(from, to, 5);

            assertTrue(buckets.isEmpty());
        }
    }

}
