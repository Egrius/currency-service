package by.Egrius.currency_service.exception.binance;

import lombok.Getter;

@Getter
public class BinanceRateLimitException extends RuntimeException {

    private final int statusCode;
    private final long retryAfterSeconds;

    public BinanceRateLimitException(int statusCode, long retryAfterSeconds) {
        super("Binance rate limit exceeded (HTTP " + statusCode + "). Retry after " + retryAfterSeconds + "s");
        this.statusCode = statusCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}