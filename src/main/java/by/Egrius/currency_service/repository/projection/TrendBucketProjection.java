package by.Egrius.currency_service.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface TrendBucketProjection {
    String getSymbol();
    BigDecimal getAveragePrice();
    Instant getMinFetchedAt();
    Instant getMaxFetchedAt();
    Integer getBucketNumber();
}