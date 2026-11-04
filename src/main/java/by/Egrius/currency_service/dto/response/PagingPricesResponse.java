package by.Egrius.currency_service.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PagingPricesResponse <T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
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