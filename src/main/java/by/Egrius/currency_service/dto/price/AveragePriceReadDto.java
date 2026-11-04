package by.Egrius.currency_service.dto.price;

import java.math.BigDecimal;

public record AveragePriceReadDto(
        BigDecimal averagePrice
) { }
