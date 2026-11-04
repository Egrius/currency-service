package by.Egrius.currency_service.mapper;

import by.Egrius.currency_service.dto.price.PriceReadDto;
import by.Egrius.currency_service.entity.Price;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface PriceReadMapper {

    PriceReadDto toReadDto(Price price);
}
