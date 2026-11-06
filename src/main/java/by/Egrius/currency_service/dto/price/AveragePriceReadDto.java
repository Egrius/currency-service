package by.Egrius.currency_service.dto.price;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;


@Schema(description = "Ответ со средней ценой за выбранный период")
public record AveragePriceReadDto(
        @Schema(description = "Рассчитанная средняя цена за интервал", example = "123.12345")
        BigDecimal averagePrice
) { }
