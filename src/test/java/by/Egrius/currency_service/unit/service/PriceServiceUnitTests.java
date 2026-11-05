package by.Egrius.currency_service.unit.service;

import by.Egrius.currency_service.dto.price.AveragePriceReadDto;
import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.dto.response.BinanceAverageResponse;
import by.Egrius.currency_service.dto.response.PagingPricesResponse;
import by.Egrius.currency_service.dto.response.TrendResponse;
import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.entity.Trend;
import by.Egrius.currency_service.exception.DateParametersMismatchException;
import by.Egrius.currency_service.exception.PriceNotFoundException;
import by.Egrius.currency_service.mapper.PriceReadMapper;
import by.Egrius.currency_service.repository.PriceRepository;
import by.Egrius.currency_service.repository.projection.TrendBucketProjection;
import by.Egrius.currency_service.service.PriceService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PriceServiceUnitTests {

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private PriceReadMapper priceReadMapper;

    @InjectMocks
    private PriceService priceService;

    private static final String SYMBOL = "BNBUSDT";

    @BeforeEach
    void setUpFields() {
        ReflectionTestUtils.setField(priceService, "pollInterval", "PT30S");
        ReflectionTestUtils.setField(priceService, "thresholdRaw", "0.01");
        ReflectionTestUtils.setField(priceService, "requestedSymbol", SYMBOL);
        ReflectionTestUtils.invokeMethod(priceService, "init");
    }

    @Nested
    class GetLastSavedPriceTests {

        @Test
        public void shouldReturnDto_whenPriceExists() {

            Price mockPrice = new Price(1L, SYMBOL, BigDecimal.ZERO, Instant.now());

            PriceReadDto expectedDto = new PriceReadDto(mockPrice.getSymbol(), mockPrice.getPrice(), mockPrice.getFetchedAt());

            when(priceRepository.findLastSavedPriceByFetchedAt())
                    .thenReturn(Optional.of(mockPrice));
            when(priceReadMapper.toReadDto(mockPrice)).thenReturn(expectedDto);

            PriceReadDto actualDto = priceService.getLastSavedPrice();

            assertNotNull(actualDto);

            assertEquals(expectedDto.fetchedAt(), actualDto.fetchedAt());
            assertEquals(expectedDto.price(), actualDto.price());
            assertEquals(expectedDto.symbol(), actualDto.symbol());
        }

        @Test
        public void shouldThrowPriceNotFoundException_whenRepositoryEmpty() {

            when(priceRepository.findLastSavedPriceByFetchedAt())
                    .thenReturn(Optional.empty());

            assertThrows(PriceNotFoundException.class, () -> priceService.getLastSavedPrice());
        }
    }

    @Nested
    class CreateFromBinanceResponseTests {

        @Test
        public void shouldSavePriceWithCorrectFieldsAndReturnDto() {
            BinanceAverageResponse response = new BinanceAverageResponse(1, BigDecimal.ZERO, 1L);
            Instant fetchedAt = Instant.now();

            Price mockPrice = new Price(1L, SYMBOL, response.price(), fetchedAt);
            PriceReadDto expectedDto = new PriceReadDto(mockPrice.getSymbol(), mockPrice.getPrice(), mockPrice.getFetchedAt());

            when(priceRepository.save(any(Price.class))).thenReturn(mockPrice);
            when(priceReadMapper.toReadDto(mockPrice)).thenReturn(expectedDto);

            PriceReadDto actualDto = priceService.createFromBinanceResponse(response, fetchedAt);

            assertNotNull(actualDto);

            assertEquals(expectedDto.fetchedAt(), actualDto.fetchedAt());
            assertEquals(expectedDto.price(), actualDto.price());
            assertEquals(expectedDto.symbol(), actualDto.symbol());
        }
    }

    @Nested
    class GetAveragePriceTests {

        @Test
        public void shouldReturnAveragePrice_whenRecordsExist() {
            Instant from = Instant.now();
            Instant to = from.plusSeconds(3600);

            BigDecimal expectedAveragePrice = new BigDecimal("605.50");
            AveragePriceReadDto expectedDto = new AveragePriceReadDto(expectedAveragePrice);

            when(priceRepository.findAveragePriceByRange(from, to)).thenReturn(Optional.of(expectedAveragePrice));

            AveragePriceReadDto actual = priceService.getAveragePrice(from, to);

            assertEquals(expectedDto.averagePrice(), actual.averagePrice());
            verify(priceRepository, times(1)).findAveragePriceByRange(from, to);
        }

        @Test
        public void shouldThrowPriceNotFoundException_whenNoRecordsInRange() {
            Instant from = Instant.now();
            Instant to = from.plusSeconds(3600);

            when(priceRepository.findAveragePriceByRange(from, to)).thenReturn(Optional.empty());

            assertThrows(PriceNotFoundException.class, () -> priceService.getAveragePrice(from, to));

            verify(priceRepository, times(1)).findAveragePriceByRange(from, to);
        }

        @Test
        public void shouldThrowDateParametersMismatch_whenFromNotBeforeTo() {
            Instant from = Instant.now();
            Instant to = from.minusSeconds(60);

            assertThrows(DateParametersMismatchException.class, () -> priceService.getAveragePrice(from, to));
            assertThrows(DateParametersMismatchException.class, () -> priceService.getAveragePrice(from, from));

            verifyNoInteractions(priceRepository);
        }
    }

    @Nested
    class GetPagingPricesByRangeTests {

        @Test
        public void shouldReturnPagedPrices_whenValidRange() {
            Instant from = Instant.now();
            Instant to = from.plusSeconds(3600);
            int page = 0;
            int size = 2;

            Price price1 = new Price(1L, SYMBOL, BigDecimal.ZERO, to);
            Price price2 = new Price(2L, SYMBOL, BigDecimal.ONE, from);

            PriceReadDto dto1 = new PriceReadDto(price1.getSymbol(), price1.getPrice(), price1.getFetchedAt());
            PriceReadDto dto2 = new PriceReadDto(price2.getSymbol(), price2.getPrice(), price2.getFetchedAt());

            Pageable expectedPageable = PageRequest.of(page, size);
            Page<Price> pricePage = new PageImpl<>(List.of(price1, price2), expectedPageable, 5L);

            when(priceRepository.findPricesByRangePageable(from, to, expectedPageable)).thenReturn(pricePage);
            when(priceReadMapper.toReadDto(price1)).thenReturn(dto1);
            when(priceReadMapper.toReadDto(price2)).thenReturn(dto2);

            PagingPricesResponse<PriceReadDto> actual = priceService.getPagingPricesByRange(from, to, page, size);

            assertNotNull(actual);
            assertEquals(2, actual.getItems().size());
            assertEquals(page, actual.getPage());
            assertEquals(size, actual.getSize());
            assertEquals(5L, actual.getTotalElements());
            assertTrue(actual.isHasNext());
        }

        @Test
        public void shouldThrowDateParametersMismatch_whenFromNotBeforeTo() {
            Instant from = Instant.now();
            Instant to = from.minusSeconds(3600);

            assertThrows(DateParametersMismatchException.class,
                    () -> priceService.getPagingPricesByRange(from, to, 0, 10));

            verifyNoInteractions(priceRepository, priceReadMapper);
        }
    }

    @Nested
    class GetTrendByRangeTests {

        private TrendBucketProjection mockBucketWithAvg(String avgPrice) {
            TrendBucketProjection bucket = mock(TrendBucketProjection.class);
            when(bucket.getAveragePrice()).thenReturn(new BigDecimal(avgPrice));
            return bucket;
        }

        @Test
        public void shouldReturnUpwardTrend_whenPriceIncreasesAboveThreshold() {
            // разница в 1 час, по коду rangedBucketCount = 5
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            // добавил 0.02%, что больше чем трешолд на 0.01
            TrendBucketProjection firstBucket = mockBucketWithAvg("100.00");
            TrendBucketProjection lastBucket = mockBucketWithAvg("100.02");

            when(priceRepository.findTrendByRange(from, to, 5))
                    .thenReturn(List.of(firstBucket, lastBucket));

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(SYMBOL, actual.symbol());
            assertEquals(from, actual.startDate());
            assertEquals(to, actual.endDate());
            assertEquals(Trend.UPWARD, actual.trend());
        }

        @Test
        public void shouldReturnDownwardTrend_whenPriceDecreasesBelowNegativeThreshold() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            // отнял 0.02%, что больше чем трешолд на 0.01
            TrendBucketProjection firstBucket = mockBucketWithAvg("100.00");
            TrendBucketProjection lastBucket = mockBucketWithAvg("99.98");

            when(priceRepository.findTrendByRange(from, to, 5))
                    .thenReturn(List.of(firstBucket, lastBucket));

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(Trend.DOWNWARD, actual.trend());
        }

        @Test
        public void shouldReturnFlatTrend_whenChangeIsWithinThreshold() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            // добавил 0.005%, что уже меньше чем трешолд на 0.01
            TrendBucketProjection firstBucket = mockBucketWithAvg("100.00");
            TrendBucketProjection lastBucket = mockBucketWithAvg("100.005");

            when(priceRepository.findTrendByRange(from, to, 5))
                    .thenReturn(List.of(firstBucket, lastBucket));

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(Trend.FLAT, actual.trend());
        }

        @Test
        public void shouldReturnFlatTrend_whenNoRecordsFound() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            when(priceRepository.findTrendByRange(from, to, 5)).thenReturn(Collections.emptyList());

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(Trend.FLAT, actual.trend());
        }

        @Test
        public void shouldRecalculateWithTwoBuckets_whenAllRecordsFallInSingleBucket() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            Instant minTime = Instant.parse("2026-01-01T00:01:00Z");
            Instant maxTime = Instant.parse("2026-01-01T00:05:00Z");

            TrendBucketProjection singleBucket = mock(TrendBucketProjection.class);
            when(singleBucket.getMinFetchedAt()).thenReturn(minTime);
            when(singleBucket.getMaxFetchedAt()).thenReturn(maxTime);

            TrendBucketProjection subBucket1 = mockBucketWithAvg("100.00");
            TrendBucketProjection subBucket2 = mockBucketWithAvg("105.00");

            when(priceRepository.findTrendByRange(from, to, 5)).thenReturn(List.of(singleBucket));
            when(priceRepository.findTrendByRange(minTime, maxTime, 2)).thenReturn(List.of(subBucket1, subBucket2));

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(Trend.UPWARD, actual.trend());

            verify(priceRepository, times(1)).findTrendByRange(from, to, 5);
            verify(priceRepository, times(1)).findTrendByRange(minTime, maxTime, 2);
        }

        @Test
        public void shouldReturnFlatWithoutRecalculation_whenSingleRecordInSingleBucket() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T01:00:00Z");

            Instant sameTime = Instant.parse("2026-01-01T00:01:00Z");

            TrendBucketProjection singleBucket = mock(TrendBucketProjection.class);
            when(singleBucket.getAveragePrice()).thenReturn(BigDecimal.ZERO);
            when(singleBucket.getMinFetchedAt()).thenReturn(sameTime);
            when(singleBucket.getMaxFetchedAt()).thenReturn(sameTime);

            List<TrendBucketProjection> bucketsMock = new ArrayList<>();
            bucketsMock.add(singleBucket);

            when(priceRepository.findTrendByRange(from, to, 5)).thenReturn(bucketsMock);

            TrendResponse actual = priceService.getTrendByRange(from, to);

            assertEquals(Trend.FLAT, actual.trend());

            verify(priceRepository, times(1)).findTrendByRange(from, to, 5);
            verifyNoMoreInteractions(priceRepository);
        }

        @Test
        public void shouldThrowDateParametersMismatch_whenFromNotBeforeTo() {
            Instant from = Instant.now();
            Instant to = from.minusSeconds(10);

            assertThrows(DateParametersMismatchException.class, () -> priceService.getTrendByRange(from, to));

            verifyNoInteractions(priceRepository);
        }
    }
}