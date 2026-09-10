package finki.ukim.mk.phone_aggregator.dto;

import java.time.LocalDateTime;

/** One observed price at one point in time, for a single source's line on the price-history chart. */
public record PricePointDto(LocalDateTime date, Integer price) {
}
