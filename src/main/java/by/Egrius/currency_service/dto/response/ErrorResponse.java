package by.Egrius.currency_service.dto.response;

import java.util.List;

public record ErrorResponse (
        String errorMessage,
        List<ErrorDto> errors
) {

    public record ErrorDto(
            String field,
            String message
    ) { }
}
