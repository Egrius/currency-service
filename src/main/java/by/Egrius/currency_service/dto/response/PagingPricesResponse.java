package by.Egrius.currency_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Schema(description = "Постраничный ответ со списком записей")
public class PagingPricesResponse <T> {

    @Schema(description = "Список элементов на текущей странице")
    private List<T> items;

    @Schema(description = "Номер текущей страницы (начиная с 0)", example = "0")
    private int page;

    @Schema(description = "Размер страницы", example = "20")
    private int size;

    @Schema(description = "Общее количество записей в запрошенном диапазоне", example = "123")
    private long totalElements;

    @Schema(description = "Есть ли следующая страница", example = "true")
    private boolean hasNext;

    public PagingPricesResponse() {

    }

    public PagingPricesResponse(List<T> items, int page, int size, long totalElements, boolean hasNext) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static <T> PagingPricesResponse<T> fromPage(Page<T> page) {
        return new PagingPricesResponse<>(
               page.getContent(),
               page.getNumber(),
               page.getSize(),
               page.getTotalElements(),
               page.hasNext()
        );
    }
}