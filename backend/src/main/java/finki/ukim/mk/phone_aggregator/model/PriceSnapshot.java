package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single price observation for an {@link Offer}, captured by a {@link ScrapeRun}.
 * Price history for an offer is the set of its snapshots ordered by scrapedAt.
 */
@Entity
@Table(name = "price_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scrape_run_id", nullable = false)
    private ScrapeRun scrapeRun;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;
}
