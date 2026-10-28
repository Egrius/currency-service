package by.Egrius.currency_service.dto.response;

import java.math.BigDecimal;

public record BinanceAverageResponseDTO(
        int mins,
        BigDecimal price,
        int lastTrade
) { }
