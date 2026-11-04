package by.Egrius.currency_service.repository;

import by.Egrius.currency_service.entity.Price;
import by.Egrius.currency_service.repository.projection.TrendProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    @Query("SELECT p FROM Price p ORDER BY p.fetchedAt DESC LIMIT 1")
    Optional<Price> findLastSavedPriceByFetchedAt();

    @Query(value = """
            SELECT AVG(p.price) FROM prices p WHERE p.fetched_at >= :from AND p.fetched_at <= :to
            """,
    nativeQuery = true)
    Optional<BigDecimal> findAveragePriceByRange(@Param("from") Instant from,
                                       @Param("to") Instant to);

    @Query("""
            SELECT p 
            FROM Price p 
            WHERE p.fetchedAt >= :from AND p.fetchedAt <= :to 
            ORDER BY p.fetchedAt DESC
    """)
    Page<Price> findPricesByRangePageable(@Param("from") Instant from,
                                          @Param("to") Instant to,
                                          Pageable pageable);

    @Query(value = """
            SELECT
                symbol as symbol,
                AVG(price) as averagePrice,
                MIN(fetched_at) as minFetchedAt,
                MAX(fetched_at) as maxFetchedAt,
                WIDTH_BUCKET(
                     EXTRACT(EPOCH FROM fetched_at),
                     EXTRACT(EPOCH FROM CAST( :from AS TIMESTAMPTZ)),
                     EXTRACT(EPOCH FROM CAST( :to AS TIMESTAMPTZ))  + 0.001,
                     :bucketsCount
                ) as bucketNumber
            FROM prices
            WHERE fetched_at >= :from AND fetched_at <= :to
            GROUP BY symbol, bucketNumber
            ORDER BY bucketNumber ASC
            """, nativeQuery = true)
    List<TrendProjection> findTrendByRange(@Param("from") Instant from,
                                           @Param("to") Instant to,
                                           @Param("bucketsCount") int bucketsCount);
}