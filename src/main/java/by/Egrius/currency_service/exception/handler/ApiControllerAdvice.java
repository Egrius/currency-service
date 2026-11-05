package by.Egrius.currency_service.exception.handler;

import by.Egrius.currency_service.controller.ApiController;
import by.Egrius.currency_service.dto.response.ErrorResponse;
import by.Egrius.currency_service.exception.DateParametersMismatchException;
import by.Egrius.currency_service.exception.PriceNotFoundException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        List<ErrorResponse.ErrorDto> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.ErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Validation failed", errors));
    }

    @ExceptionHandler(value = HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {

        List<ErrorResponse.ErrorDto> errors = new ArrayList<>();

        for(ParameterValidationResult result : e.getParameterValidationResults()) {
            String param = result.getMethodParameter().getParameterName();

            if (param == null) continue;

            String msg = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));

            errors.add(new ErrorResponse.ErrorDto(param, msg));
        }

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Validation failed", errors));
    }


    @ExceptionHandler(value = DateParametersMismatchException.class)
    public ResponseEntity<ErrorResponse> handleDateParametersMismatchException(DateParametersMismatchException e) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(e.getMessage(), List.of()));
    }

    @ExceptionHandler(value = PriceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePriceNotFoundException(PriceNotFoundException e) {
        return ResponseEntity
                .status(HttpStatusCode.valueOf(404))
                .body(new ErrorResponse(e.getMessage(), List.of()));
    }
}
