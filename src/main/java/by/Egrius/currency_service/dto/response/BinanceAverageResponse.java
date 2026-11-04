package by.Egrius.currency_service.dto.response;

import java.math.BigDecimal;

public record BinanceAverageResponse(
        int mins,
        BigDecimal price,
        long closeTime
) { }
