package by.Egrius.currency_service.mapper;

import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.entity.Price;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-05T20:19:37+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (IBM Corporation)"
)
@Component
public class PriceReadMapperImpl implements PriceReadMapper {

    @Override
    public PriceReadDto toReadDto(Price price) {
        if ( price == null ) {
            return null;
        }

        String symbol = null;
        BigDecimal price1 = null;
        Instant fetchedAt = null;

        symbol = price.getSymbol();
        price1 = price.getPrice();
        fetchedAt = price.getFetchedAt();

        PriceReadDto priceReadDto = new PriceReadDto( symbol, price1, fetchedAt );

        return priceReadDto;
    }
}
