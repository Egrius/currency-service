package by.Egrius.currency_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;


@Schema(description = "Ответ при ошибке")
public record ErrorResponse (

        @Schema(description = "Основное сообщение об ошибке", example = "Validation failed")
        String errorMessage,

        @Schema(description = "Список ошибок валидации полей/параметров")
        List<ErrorDto> errors
) {

    @Schema(description = "Ошибка конкретного параметра или поля")
    public record ErrorDto(
            @Schema(description = "Имя параметра или поля", example = "page")
            String field,

            @Schema(description = "Описание ошибки", example = "You can't provide param 'page' with a value less than '0'")
            String message
    ) { }
}
