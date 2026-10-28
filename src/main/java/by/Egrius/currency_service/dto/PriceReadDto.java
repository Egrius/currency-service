package by.Egrius.currency_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceReadDto(
        String symbol,
        BigDecimal price,
        Instant fetchedAt
) { }
