package by.Egrius.currency_service.service;

import by.Egrius.currency_service.dto.PriceReadDto;
import by.Egrius.currency_service.dto.response.BinanceAverageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BinanceScheduler {

    @Value("${binance.symbol}")
    private String requestedSymbol;

    private final RestClient restClient;

    private final PriceService priceService;

    @Scheduled(fixedRateString = "${binance.poll-interval}")
    public void requestAvgPriceFromApi() {
        // Делает запрос каждые poll-interval
        // Сохраняет в БД полученные записи

        // Ошибки со стороны запроса к апи не должны останавливать расписание (try-catch)
        // Без дублей в бд (constraint / проверка)
        try {
            Instant fetchedAt = Instant.now();

            BinanceAverageResponseDTO response = restClient.get()
                    .uri(uri ->
                            uri
                                    .path("/avgPrice")
                                    .queryParam("symbol", requestedSymbol)
                                    .build()
                    )
                    .retrieve()
                    .body(BinanceAverageResponseDTO.class);

            if (response != null) {
                priceService.createFromBinanceResponse(response, fetchedAt);
            } else {
                // можно лог для пустого ответа
            }

        } catch (Exception e) {

        }




    }
}
