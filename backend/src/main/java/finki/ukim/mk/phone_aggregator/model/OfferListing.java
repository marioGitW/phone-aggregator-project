package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Read-only projection of every currently-active {@link Offer}, backed by the
 * {@code offer_listings} database view (see V1 migration), which joins each active
 * offer to its {@link PhoneModel} and its latest {@link PriceSnapshot}.
 * <p>
 * "price" isn't a persisted column anywhere - it's derived per-offer from price history -
 * so listing/filtering/sorting/paginating by it (the {@code GET /api/phones} use case)
 * is delegated to Postgres via this view instead of being reconstructed in JPA Criteria.
 */
@Entity
@Table(name = "offer_listings")
@Immutable
@Data
@NoArgsConstructor
public class OfferListing {

    @Id
    private Long id;

    @Column(name = "phone_model_id")
    private Long phoneModelId;

    private String brand;

    private String title;

    @Column(name = "raw_title")
    private String rawTitle;

    @Column(name = "site_link")
    private String siteLink;

    private Integer price;

    @Column(name = "image_url")
    private String imageUrl;

    private String source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "color_raw")
    private String colorRaw;

    @Column(name = "color_canonical")
    private String colorCanonical;
}
