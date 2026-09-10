package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
