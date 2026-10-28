package by.Egrius.currency_service.service;

import by.Egrius.currency_service.dto.PriceReadDto;
import by.Egrius.currency_service.dto.response.BinanceAverageResponseDTO;
import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceService {


    private final PriceRepository priceRepository;

    public PriceReadDto getLastSavedPrice() {
        return null;
    }

    @Transactional
    public PriceReadDto createFromBinanceResponse(BinanceAverageResponseDTO response, Instant fetchedAt) {
        Price price = Price.builder()
                .price(response.price())
                .symbol(requestedSymbol)
                .fetchedAt(fetchedAt)
                .build();

    }



}