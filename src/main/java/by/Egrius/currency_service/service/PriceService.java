package by.Egrius.currency_service.service;

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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceService {

    @Value("${binance.poll-interval}")
    private String pollInterval;

    @Value("${binance.trend.threshold}")
    private String thresholdRaw;

    @Value("${binance.symbol}")
    private String requestedSymbol;

    private BigDecimal threshold;

    private static final int MIN_BUCKETS_COUNT = 2;

    private final PriceRepository priceRepository;
    private final PriceReadMapper priceReadMapper;



    @PostConstruct
    void init() {
        this.threshold = new BigDecimal(thresholdRaw);
        log.info("Initialized Price Service with: symbol={}, pollInterval={}, trendThreshold={}%",
                requestedSymbol, pollInterval, threshold);
    }

    public PriceReadDto getLastSavedPrice() {
        log.debug("Searching for last saved price for symbol={}", requestedSymbol);

        Price lastSavedPrice = priceRepository.findLastSavedPriceByFetchedAt()
                .orElseThrow(() -> new PriceNotFoundException("Unable to find last saved Price"));

        return priceReadMapper.toReadDto(lastSavedPrice);
    }

    // 1
    @Transactional
    public PriceReadDto createFromBinanceResponse(BinanceAverageResponse response,
                                                  Instant fetchedAt) {
        Price price = Price.builder()
                .price(response.price())
                .symbol(requestedSymbol)
                .fetchedAt(fetchedAt)
                .build();

        Price saved = priceRepository.save(price);

        log.info("Saved new price: symbol={}, price={}, fetchedAt={}",
                saved.getSymbol(), saved.getPrice(), saved.getFetchedAt());

        return priceReadMapper.toReadDto(saved);
    }

    // 2
    public AveragePriceReadDto getAveragePrice(Instant from, Instant to) {

        validateDateRange(from, to);

        BigDecimal average = priceRepository.findAveragePriceByRange(from, to)
                .orElseThrow(() -> new PriceNotFoundException(
                        "No prices found in range from " + from + " to " + to));

        return new AveragePriceReadDto(average);
    }

    public PagingPricesResponse<PriceReadDto> getPagingPricesByRange(Instant from, Instant to, int page, int size) {

        validateDateRange(from, to);

        Pageable pageable = PageRequest.of(page, size);

        log.debug("Searching for paged prices from={} to={}, page={}, size={}", from, to, page, size);

        return PagingPricesResponse.fromPage(
                priceRepository.findPricesByRangePageable(from, to, pageable)
                .map(priceReadMapper::toReadDto)
        );
    }

    public TrendResponse getTrendByRange(Instant from, Instant to) {

        validateDateRange(from, to);
        /*
        пришел from и to
        шедуллер пишет раз в binance.poll-interval

        значит колиество записей (примерно): (to - from) / binance.poll-interval
        нужно понять, какая разница между to и from? час, день, месяц, год - от этого зависит количество корзин
         */
        Duration duration = Duration.between(from, to);

        int rangedBucketCount = 25;

        if(duration.getSeconds() <= Duration.ofHours(1).getSeconds()) rangedBucketCount = 5;
        else if(duration.toMinutes() <= Duration.ofDays(1).toMinutes()) rangedBucketCount = 10;
        else if(duration.toHours() <= Duration.ofDays(30).toHours()) rangedBucketCount = 15;

        long pollSeconds = Duration.parse(pollInterval).getSeconds();

        long calculatedBucketCount = pollSeconds > 0
                ? duration.getSeconds() / pollSeconds
                : (long) rangedBucketCount;

        int bucketsCount = (int) Math.max(MIN_BUCKETS_COUNT, Math.min(calculatedBucketCount, rangedBucketCount));

        log.debug("Calculating trend from={} to={} using bucketsCount={}", from, to, bucketsCount);

        List<TrendBucketProjection> buckets = priceRepository.findTrendByRange(from, to, bucketsCount);

        if(buckets.size() == 1) {

            TrendBucketProjection singleBucket = buckets.getFirst();
            Instant minTime = singleBucket.getMinFetchedAt();
            Instant maxTime = singleBucket.getMaxFetchedAt();

            if (minTime != null && maxTime != null && minTime.isBefore(maxTime)) {

                log.debug("All of the records are contained in a single bucket [{} .. {}]. Started recalculation for new {} sub-buckets",
                        minTime, maxTime, MIN_BUCKETS_COUNT);

                buckets = priceRepository.findTrendByRange(minTime, maxTime, MIN_BUCKETS_COUNT);
            }
        }

        Trend trend = calculateTrend(buckets);

        log.info("Calculated trend for symbol={} for the range of [{} .. {}]: {}", requestedSymbol, from, to, trend);

        return new TrendResponse(requestedSymbol, from, to, trend);
    }

    private Trend calculateTrend(List<TrendBucketProjection> buckets) {
        /*
        Пришли корзины: максимальное количество их может быть bucketsCount + 1 (специфика функции)
        Upd: теперь без +1, исправил путём добавления 0.001 к верхней границе
        Upd 2.0: +1 исправил через LEAST() SQL-функцию, добавление 0.001 приводило к ошибкам взятия среднего,
        т.к смещало все бакеты, а задача была защититься от создания доп бакета, если время записи точь-в-точь
        как правая граница

        в пустых корзинах данных нет - не попали в определенный пустой корзиной промежуток => они не ориентир.
        заполненные - представляют собой среднее по интервалам.

        Задача -> проанализировать, какой тренд в итоге, т.е прийти к одному числу

        Т.е. определить какой процент от начального составляет разница
         */

        if(buckets.isEmpty()) return Trend.FLAT;

        BigDecimal first = buckets.getFirst().getAveragePrice();
        BigDecimal last = buckets.getLast().getAveragePrice();

        // Нестандартная ситуация, когда < 0, оставил на подстраховку
        if (first.compareTo(BigDecimal.ZERO) <= 0) {
            if(last.compareTo(BigDecimal.ZERO) == 0) return Trend.FLAT;
            else if(last.compareTo(BigDecimal.ZERO) > 0) return Trend.UPWARD;
            else if(last.compareTo(BigDecimal.ZERO) < 0) return Trend.DOWNWARD; // Нестандартная ситуация тоже
        }

        BigDecimal diff = last.subtract(first);

        BigDecimal diffPercentage = diff.abs()
                .multiply(BigDecimal.valueOf(100L))
                .divide(first, 5, RoundingMode.HALF_UP);
        /*
        после процента понять, больше он или меньше трешолда, после - направление
         */
        if(diffPercentage.compareTo(threshold) >= 0) {

            // здесь уже направление, т.е мы в минусе или в плюсе
            if(diff.compareTo(BigDecimal.ZERO) < 0) return Trend.DOWNWARD;
            else if(diff.compareTo(BigDecimal.ZERO) > 0) return Trend.UPWARD;
        }
        return Trend.FLAT;
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new DateParametersMismatchException(
                    "Invalid date range: 'from' (" + from + ") must be before 'to' (" + to + ")");
        }
    }
}