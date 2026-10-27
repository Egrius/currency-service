package by.Egrius.currency_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "prices",
        uniqueConstraints = {
        @UniqueConstraint(name = "symbol_fetched_at_unique", columnNames = {"symbol", "fetched_at"})
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", length = 50, nullable = false)
    private String symbol;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price other)) return false;

        if (id != null && other.id != null) {
            return Objects.equals(id, other.id);
        }
        return Objects.equals(symbol, other.symbol)
                && Objects.equals(fetchedAt, other.fetchedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, fetchedAt);
    }
}
