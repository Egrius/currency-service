package by.Egrius.currency_service.dto.response;

import by.Egrius.currency_service.entity.Trend;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Результат расчёта ценового тренда за интервал")
public record TrendResponse(

        @Schema(description = "Символ торговой пары", example = "BNBUSDT")
        String symbol,

        @Schema(
                name = "start_date",
                description = "Дата начала интервала (UTC)",
                type = "string",
                format = "date",
                example = "2026-01-01"
        )
        @JsonProperty("start_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
        Instant startDate,

        @Schema(
                name = "end_date",
                description = "Дата конца интервала (UTC)",
                type = "string",
                format = "date",
                example = "2026-01-30"
        )
        @JsonProperty("end_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
        Instant endDate,

        @Schema(description = "Тренд", example = "UPWARD")
        Trend trend
) { }
