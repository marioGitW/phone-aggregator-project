package finki.ukim.mk.phone_aggregator.dto;

/** One color variant of a (model, source) offer group - the actual clickable listing. */
public record ColorOfferDto(
        Long offerId,
        String colorRaw,
        String colorCanonical,
        Integer price,
        String siteLink,
        String imageUrl
) {
}
