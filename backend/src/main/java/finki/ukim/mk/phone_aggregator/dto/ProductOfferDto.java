package finki.ukim.mk.phone_aggregator.dto;

import java.time.LocalDateTime;

public record ProductOfferDto(
        Long id,
        String brand,
        String title,
        Integer price,
        String source,
        String siteLink,
        String imageUrl,
        LocalDateTime createdAt
) {
}

