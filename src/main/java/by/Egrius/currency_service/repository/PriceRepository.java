package by.Egrius.currency_service.repository;

import by.Egrius.currency_service.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<Price, Long> {

}