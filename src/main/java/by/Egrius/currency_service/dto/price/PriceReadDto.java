package by.Egrius.currency_service.dto.price;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Информация о сохраненной цене торговой пары")
public record PriceReadDto(
        @Schema(description = "Символ торговой пары", example = "BNBUSDT")
        String symbol,

        @Schema(description = "Цена торговой пары", example = "605.12345678")
        BigDecimal price,

        @Schema(
                description = "Время получения  записи от стороннего API в формате ISO-8601 в UTC (YYYY-MM-DDTHH:mm:ssZ)",
                type = "string",
                example = "2026-01-01T00:00:00Z"
        )
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant fetchedAt
) { }
