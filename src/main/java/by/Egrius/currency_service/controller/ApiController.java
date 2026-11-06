package by.Egrius.currency_service.controller;

import by.Egrius.currency_service.constant.PaginationConstants;
import by.Egrius.currency_service.dto.price.AveragePriceReadDto;
import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.dto.response.ErrorResponse;
import by.Egrius.currency_service.dto.response.PagingPricesResponse;
import by.Egrius.currency_service.dto.response.TrendResponse;
import by.Egrius.currency_service.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@Tag(name = "Prices API controller", description = "Реализованные методы для работы")
public class ApiController {

    private final PriceService priceService;

    // Получить последнюю запрошенную шедуллером запись
    @Operation(
            summary = "Получить последнюю сохраненную цену",
            description = "Возвращает последнюю сохранённую запись о цене торговой пары, полученную от Binance API"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Последняя запись успешно получена"),
            @ApiResponse(
                    responseCode = "404",
                    description = "В базе данных ещё нет сохранённых записей",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/last-saved")
    public PriceReadDto getLastSavedRecord() {
        return priceService.getLastSavedPrice();
    }

    // Средняя цена по интервалу (включительно)
    @Operation(
            summary = "Рассчитать среднюю цену за период",
            description = "Вычисляет среднее значение цены торговой пары в заданном временном интервале [from, to] включительно"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Средняя цена успешно рассчитана"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный интервал дат (например, from >= to) или неверный формат даты",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Записи в указанном интервале времени не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/average")
    public AveragePriceReadDto getAveragePriceByRange(
                                                    @Parameter(description = "Начало интервала в формате ISO-8601 UTC", example = "2026-01-01T00:00:00Z", required = true)
                                                    @RequestParam("from")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant from,

                                                    @Parameter(description = "Конец интервала в формате ISO-8601 UTC", example = "2026-12-30T00:00:00Z", required = true)
                                                    @RequestParam("to")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant to
    ) {
        return priceService.getAveragePrice(from, to);
    }

    @Operation(
            summary = "Получить список записей за период с пагинацией",
            description = "Возвращает страницу сохранённых в заданном временном интервале [from, to], отсортированных по убыванию времени получения"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница с записями успешно получена"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные параметры пагинации (page < 0, size < 1 или size > 100) или интервала дат",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public PagingPricesResponse<PriceReadDto> getPagingPricesByRange(
                                                    @Parameter(description = "Начало интервала в формате ISO-8601 UTC", example = "2026-01-01T00:00:00Z", required = true)
                                                    @RequestParam("from")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant from,

                                                    @Parameter(description = "Конец интервала в формате ISO-8601 UTC", example = "2026-12-30T00:00:00Z", required = true)
                                                    @RequestParam("to")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant to,

                                                    @Parameter(description = "Номер страницы (начиная с 0)", example = "0", required = true)
                                                    @RequestParam(value = "page", defaultValue = "0")
                                                    @Min(value = 0, message = "You can't provide param 'page' with a value less than '0'")
                                                    Integer page,

                                                    @Parameter(description = "Размер страницы (от 1 до 100)", example = "20")
                                                    @RequestParam(value = "size", defaultValue = "" + PaginationConstants.DEFAULT_PAGE_SIZE)
                                                    @Min(value = 1, message = "You can't provide param 'size' with a value less than '1'")
                                                    @Max(value = 100, message = "You can't provide param 'size' with a value greater than '100'")
                                                    Integer size
    ) {

        return priceService.getPagingPricesByRange(from, to, page, size);
    }

    // Получить тренд по интервалу
    @Operation(
            summary = "Определить тренд цены за период",
            description = "Вычисляет направление ценового тренда (UPWARD, DOWNWARD, FLAT) в заданном интервале [from, to]"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Тренд успешно рассчитан"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный интервал дат (from >= to)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/trend")
    public TrendResponse getTrendByRange(
                                    @Parameter(description = "Начало интервала в формате ISO-8601 UTC", example = "2026-01-01T00:00:00Z", required = true)
                                    @RequestParam("from")
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    Instant from,

                                    @Parameter(description = "Конец интервала в формате ISO-8601 UTC", example = "2026-12-30T00:00:00Z", required = true)
                                    @RequestParam("to")
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    Instant to
    ) {
        return priceService.getTrendByRange(from, to);
    }
}
