package by.Egrius.currency_service.controller;

import by.Egrius.currency_service.constant.PaginationConstants;
import by.Egrius.currency_service.dto.price.AveragePriceReadDto;
import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.dto.response.PagingPricesResponse;
import by.Egrius.currency_service.dto.response.TrendResponse;
import by.Egrius.currency_service.service.PriceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class ApiController {

    private final PriceService priceService;

    // Получить последнюю запрошенную шедуллером запись
    @GetMapping("/last-saved")
    public PriceReadDto getLastSavedRecord() {
        return priceService.getLastSavedPrice();
    }

    // Средняя цена по интервалу (включительно)
    @GetMapping("/average")
    public AveragePriceReadDto getAveragePriceByRange(
                                                    @RequestParam("from")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant from,

                                                    @RequestParam("to")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant to
    ) {
        return priceService.getAveragePrice(from, to);
    }

    @GetMapping
    public PagingPricesResponse<PriceReadDto> getPagingPricesByRange(
                                                    @RequestParam("from")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant from,

                                                    @RequestParam("to")
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    Instant to,

                                                    @RequestParam(value = "page", defaultValue = "0")
                                                    @Min(value = 0, message = "You can't provide param 'page' with a value less than '0'")
                                                    Integer page,

                                                    @RequestParam(value = "size", defaultValue = "" + PaginationConstants.DEFAULT_PAGE_SIZE)
                                                    @Min(value = 1, message = "You can't provide param 'size' with a value less than '1'")
                                                    @Max(value = 100, message = "You can't provide param 'size' with a value greater than '100'")
                                                    Integer size
    ) {

        return priceService.getPagingPricesByRange(from, to, page, size);
    }

    // Получить тренд по интервалу
    @GetMapping("/trend")
    public TrendResponse getTrendByRange(
                                    @RequestParam("from")
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    Instant from,

                                     @RequestParam("to")
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                     Instant to
    ) {
        return priceService.getTrendByRange(from, to);
    }
}
