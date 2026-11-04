package by.Egrius.currency_service.dto.response;

import by.Egrius.currency_service.entity.Trend;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TrendResponse(
        String symbol,

        @JsonProperty("start_date")
        Instant startDate,

        @JsonProperty("end_date")
        Instant endDate,
        Trend trend
) { }
