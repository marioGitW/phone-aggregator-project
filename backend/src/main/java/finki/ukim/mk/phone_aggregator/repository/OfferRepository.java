package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.dto.SourceCountDto;
import finki.ukim.mk.phone_aggregator.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    /**
     * Mirrors the ux_offers_source_variant_or_link unique index exactly: an offer's
     * real identity within a source is its variant_key when present, otherwise its
     * site_link.
     */
    @Query("SELECT o FROM Offer o WHERE o.source = :source " +
            "AND COALESCE(o.variantKey, o.siteLink) = COALESCE(:variantKey, :siteLink)")
    Optional<Offer> findBySourceAndIdentityKey(@Param("source") String source,
                                                @Param("variantKey") String variantKey,
                                                @Param("siteLink") String siteLink);

    @Query("SELECT DISTINCT o.source FROM Offer o ORDER BY o.source")
    List<String> findDistinctSources();

    @Query("SELECT new finki.ukim.mk.phone_aggregator.dto.SourceCountDto(o.source, COUNT(o.id)) " +
            "FROM Offer o WHERE o.isActive = true GROUP BY o.source ORDER BY o.source")
    List<SourceCountDto> getListingsPerSource();

    /**
     * Offers not touched by the current scrape run (i.e. absent from the latest full
     * scrape) are no longer listed anywhere and get flipped inactive.
     */
    @Modifying
    @Query("UPDATE Offer o SET o.isActive = false WHERE o.lastSeenAt < :cutoff AND o.isActive = true")
    int deactivateStaleOffers(@Param("cutoff") LocalDateTime cutoff);

    // --- Analytics: these need each offer's *latest* price, which isn't a persisted
    // column, so they're native SQL (DISTINCT ON) rather than JPQL. Rows are mapped to
    // DTOs in PhoneService.

    @Query(value = """
            SELECT pm.brand, CAST(AVG(latest.price) AS BIGINT)
            FROM offers o
            JOIN phone_models pm ON pm.id = o.phone_model_id
            JOIN (
                SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
                FROM price_snapshots ps
                ORDER BY ps.offer_id, ps.scraped_at DESC
            ) latest ON latest.offer_id = o.id
            WHERE o.is_active = true
            GROUP BY pm.brand
            ORDER BY pm.brand
            """, nativeQuery = true)
    List<Object[]> getAveragePriceByBrandRaw();

    @Query(value = """
            SELECT o.source, latest.price
            FROM offers o
            JOIN phone_models pm ON pm.id = o.phone_model_id
            JOIN (
                SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
                FROM price_snapshots ps
                ORDER BY ps.offer_id, ps.scraped_at DESC
            ) latest ON latest.offer_id = o.id
            WHERE o.is_active = true AND pm.model_key = :modelKey
            ORDER BY o.source
            """, nativeQuery = true)
    List<Object[]> getPricesBySourceForModelKeyRaw(@Param("modelKey") String modelKey);

    /**
     * The single cheapest active offer per brand, in one pass (no N+1 across brands).
     */
    @Query(value = """
            WITH latest AS (
                SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
                FROM price_snapshots ps
                ORDER BY ps.offer_id, ps.scraped_at DESC
            ),
            ranked AS (
                SELECT o.id, pm.brand, pm.display_name, latest.price, o.image_url, o.source, o.site_link,
                       ROW_NUMBER() OVER (PARTITION BY pm.brand ORDER BY latest.price ASC, o.id ASC) AS rn
                FROM offers o
                JOIN phone_models pm ON pm.id = o.phone_model_id
                JOIN latest ON latest.offer_id = o.id
                WHERE o.is_active = true
            )
            SELECT id, brand, display_name, price, image_url, source, site_link
            FROM ranked
            WHERE rn = 1
            ORDER BY brand
            """, nativeQuery = true)
    List<Object[]> getCheapestOfferByBrandRaw();

    @Query(value = """
            WITH latest AS (
                SELECT DISTINCT ON (ps.offer_id) ps.offer_id, ps.price
                FROM price_snapshots ps
                ORDER BY ps.offer_id, ps.scraped_at DESC
            )
            SELECT
                CASE
                    WHEN latest.price < 10000 THEN '0-10k'
                    WHEN latest.price < 20000 THEN '10k-20k'
                    WHEN latest.price < 30000 THEN '20k-30k'
                    ELSE '30k+'
                END AS price_range,
                COUNT(*),
                MIN(CASE
                    WHEN latest.price < 10000 THEN 1
                    WHEN latest.price < 20000 THEN 2
                    WHEN latest.price < 30000 THEN 3
                    ELSE 4
                END) AS sort_order
            FROM offers o
            JOIN latest ON latest.offer_id = o.id
            WHERE o.is_active = true
            GROUP BY price_range
            ORDER BY sort_order
            """, nativeQuery = true)
    List<Object[]> getPriceDistributionRaw();
}
