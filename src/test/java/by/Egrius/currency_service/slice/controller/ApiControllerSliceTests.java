package by.Egrius.currency_service.slice.controller;

import by.Egrius.currency_service.controller.ApiController;
import by.Egrius.currency_service.dto.price.AveragePriceReadDto;
import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.dto.response.PagingPricesResponse;
import by.Egrius.currency_service.dto.response.TrendResponse;
import by.Egrius.currency_service.entity.Trend;
import by.Egrius.currency_service.exception.DateParametersMismatchException;
import by.Egrius.currency_service.exception.PriceNotFoundException;
import by.Egrius.currency_service.exception.handler.ApiControllerAdvice;
import by.Egrius.currency_service.service.PriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
@Import(ApiControllerAdvice.class)
class ApiControllerSliceTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceService priceService;

    private static final String BASE_URL = "/api/v1/prices";
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-30T00:00:00Z");

    @Nested
    @DisplayName("GET /api/v1/prices/last-saved")
    class GetLastSavedRecordTests {
        @Test
        void getLastSavedRecord_shouldReturn200AndFormattedJson_whenRecordExists() throws Exception {
            Instant fetchedAt = Instant.parse("2026-09-15T14:30:45Z");
            PriceReadDto dto = new PriceReadDto("BNBUSDT", new BigDecimal("590.12345678"), fetchedAt);

            when(priceService.getLastSavedPrice()).thenReturn(dto);


            mockMvc.perform(get(BASE_URL + "/last-saved")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                    .andExpect(jsonPath("$.price").value(590.12345678))
                    .andExpect(jsonPath("$.fetchedAt").value(fetchedAt.toString()));

            verify(priceService).getLastSavedPrice();
        }

        @Test
        void getLastSavedRecord_shouldReturn404_whenPriceNotFound() throws Exception {
            when(priceService.getLastSavedPrice())
                    .thenThrow(new PriceNotFoundException("No saved prices found"));

            mockMvc.perform(get(BASE_URL + "/last-saved")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorMessage").exists());

            verify(priceService).getLastSavedPrice();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/prices/average")
    class GetAveragePriceByRangeTests {

        @Test
        void getAveragePriceByRange_shouldReturn200_whenRangeIsValid() throws Exception {
            AveragePriceReadDto dto = new AveragePriceReadDto(new BigDecimal("10.2"));
            when(priceService.getAveragePrice(FROM, TO)).thenReturn(dto);

            mockMvc.perform(get(BASE_URL + "/average")
                            .param("from", FROM.toString())
                            .param("to", TO.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averagePrice").value(10.2));

            verify(priceService).getAveragePrice(FROM, TO);
        }

        @Test
        void getAveragePriceByRange_shouldReturn400_whenDatesMismatch() throws Exception {
            when(priceService.getAveragePrice(TO, FROM))
                    .thenThrow(new DateParametersMismatchException("'from' must be before 'to'"));

            mockMvc.perform(get(BASE_URL + "/average")
                            .param("from", TO.toString())
                            .param("to", FROM.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorMessage").value("'from' must be before 'to'"));

            verify(priceService).getAveragePrice(TO, FROM);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/prices")
    class GetPagingPricesByRangeTests {

        @Test
        void getPagingPricesByRange_shouldReturn200AndPageStructure_whenValidParams() throws Exception {
            PriceReadDto item = new PriceReadDto("BNBUSDT", new BigDecimal("600.00"), FROM);
            PagingPricesResponse<PriceReadDto> response = new PagingPricesResponse<>(
                    List.of(item), 0, 20, 135L, true
            );

            when(priceService.getPagingPricesByRange(FROM, TO, 0, 20)).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .param("from", FROM.toString())
                            .param("to", TO.toString())
                            .param("page", "0")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items[0].symbol").value("BNBUSDT"))
                    .andExpect(jsonPath("$.items[0].price").value(600.00))
                    .andExpect(jsonPath("$.items[0].fetchedAt").value(FROM.toString()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(135))
                    .andExpect(jsonPath("$.hasNext").value(true));

            verify(priceService).getPagingPricesByRange(FROM, TO, 0, 20);
        }

        @Test
        void getPagingPricesByRange_shouldReturn400_whenPageIsNegativeOrSizeOutOfBounds() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("from", FROM.toString())
                            .param("to", TO.toString())
                            .param("page", "-1")
                            .param("size", "101")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorMessage").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isNotEmpty());

            verifyNoInteractions(priceService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/prices/trend")
    class GetTrendByRangeTests {

        @Test
        void getTrendByRange_shouldReturn200WithSnakeCaseDates_whenRangeIsValid() throws Exception {
            TrendResponse response = new TrendResponse("BNBUSDT", FROM, TO, Trend.UPWARD);
            when(priceService.getTrendByRange(FROM, TO)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/trend")
                            .param("from", FROM.toString())
                            .param("to", TO.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                    .andExpect(jsonPath("$.start_date").value("2026-01-01"))
                    .andExpect(jsonPath("$.end_date").value("2026-01-30"))
                    .andExpect(jsonPath("$.trend").value("UPWARD"));

            verify(priceService).getTrendByRange(FROM, TO);
        }
    }
}