package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    /**
     * Snapshot count per offer, as (offerId, count) pairs. Used to verify import
     * idempotency: after N imports of the same payload, every offer should have exactly
     * N snapshots - no more, no fewer.
     */
    @Query("SELECT ps.offer.id, COUNT(ps) FROM PriceSnapshot ps GROUP BY ps.offer.id")
    List<Object[]> countGroupedByOfferId();

    /**
     * Every snapshot ever recorded for offers of the given phone model, as
     * (source, scrapedAt, price) rows ordered for direct grouping into one series per
     * source. Deliberately not restricted to isActive offers - this is a historical price
     * record, not a "what can I buy right now" view (that's OfferListingRepository).
     */
    @Query("SELECT o.source, ps.scrapedAt, ps.price " +
            "FROM PriceSnapshot ps JOIN ps.offer o " +
            "WHERE o.phoneModel.id = :phoneModelId " +
            "ORDER BY o.source ASC, ps.scrapedAt ASC")
    List<Object[]> findPriceHistoryByPhoneModelId(@Param("phoneModelId") Long phoneModelId);
}
