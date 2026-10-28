package by.Egrius.currency_service.controller;

import by.Egrius.currency_service.dto.PriceReadDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//TODO шедуллер делает запрос через RstClient на апи, сохраняет в БД

@RestController
@RequestMapping("/api")
public class ApiController {

    // Получить последнюю запрошенную шедуллером запись
    @GetMapping("/last-saved-record")
    public ResponseEntity<PriceReadDto> getLastSavedRecord() {

    }
}
