package by.Egrius.currency_service.dto.response;

import by.Egrius.currency_service.entity.Trend;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TrendResponse(
        String symbol,

        @JsonProperty("start_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
        Instant startDate,

        @JsonProperty("end_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
        Instant endDate,

        Trend trend
) { }
