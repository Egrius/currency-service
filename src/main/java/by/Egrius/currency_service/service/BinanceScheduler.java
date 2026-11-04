package by.Egrius.currency_service.service;

import by.Egrius.currency_service.dto.response.BinanceAverageResponse;
import by.Egrius.currency_service.exception.binance.BinanceRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceScheduler {

    @Value("${binance.symbol}")
    private String requestedSymbol;

    private static final long DEFAULT_BACKOFF_SECONDS = 60L;

    private final RestClient restClient;

    private final PriceService priceService;

    private volatile Instant backOffUntil = Instant.MIN;

    @Scheduled(fixedRateString = "${binance.poll-interval}")

    public void requestAvgPriceFromApi() {
        // Делает запрос каждые poll-interval
        // Сохраняет в БД полученные записи

        // Ошибки со стороны запроса к апи не должны останавливать расписание (try-catch)
        // Без дублей в бд (constraint / проверка)
        Instant serverTime = Instant.now();

        if(serverTime.isBefore(backOffUntil)) {
            log.warn("Skipping Binance fetch due to the rate limit backoff until {}", backOffUntil);
            return;
        }

        try {

            log.info("Fetching /avgPrice");

            BinanceAverageResponse binanceResponse = restClient.get()
                    .uri(uri ->
                            uri
                                    .path("/avgPrice")
                                    .queryParam("symbol", requestedSymbol)
                                    .build()
                    )
                    .retrieve()
                    .onStatus(status -> status.value() == 429 || status.value() == 418,
                            (request, response) -> {

                            long retryAfterSeconds = parseRetryAfter(response.getHeaders());
                            throw new BinanceRateLimitException(response.getStatusCode().value(), retryAfterSeconds);
                    })
                    .body(BinanceAverageResponse.class);


            log.info("Fetched result: {}", binanceResponse);

            if (binanceResponse == null) {
                log.warn("Received empty response from Binance for symbol={}", requestedSymbol);
            } else if (binanceResponse.price() == null) {
                log.warn("Received empty price from Binance for symbol={}", requestedSymbol);
            } else {
                priceService.createFromBinanceResponse(binanceResponse, serverTime);
            }
            // ошибки 429 и 418
        } catch (BinanceRateLimitException e) {
            backOffUntil = serverTime.plusSeconds(e.getRetryAfterSeconds());
            log.warn("Failed to fetch from Binance due to the rate limits, backing off until {}", backOffUntil);

            // остальные ошибки в ответе
        } catch (RestClientResponseException e) {

            log.error("Binance API returned error HTTP {}: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());

            // остальные, чтобы шедуллер не падал
        } catch (Exception e) {
            log.error("Failed to fetch or save price from Binance, failed with message: {}", e.getMessage());
        }
    }

    private long parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter.trim());
            } catch (NumberFormatException e) {
                log.warn("Could not parse 'Retry-After' from response: {}", retryAfter);
            }
        }
        return DEFAULT_BACKOFF_SECONDS;
    }
}
