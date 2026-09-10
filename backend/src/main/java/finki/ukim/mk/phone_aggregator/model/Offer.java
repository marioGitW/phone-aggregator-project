package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One source's listing of a {@link PhoneModel} - a specific site link selling a specific
 * color/variant. Price history lives separately in {@link PriceSnapshot}.
 */
@Entity
@Table(name = "offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phone_model_id", nullable = false)
    private PhoneModel phoneModel;

    @Column(nullable = false)
    private String source;

    @Column(name = "site_link", nullable = false, columnDefinition = "TEXT")
    private String siteLink;

    @Column(name = "variant_key")
    private String variantKey;

    @Column(name = "raw_title", nullable = false, columnDefinition = "TEXT")
    private String rawTitle;

    @Column(name = "color_raw")
    private String colorRaw;

    @Column(name = "color_canonical")
    private String colorCanonical;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "match_strategy", nullable = false, length = 32)
    private MatchStrategy matchStrategy;

    @Column(name = "match_confidence", nullable = false)
    private Double matchConfidence;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
