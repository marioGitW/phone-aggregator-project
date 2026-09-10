package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.PhoneModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneModelRepository extends JpaRepository<PhoneModel, Long> {

    /**
     * Tier 1 candidates: exact (brand, modelCode) - e.g. a Samsung SM- code. storageGb is
     * intentionally excluded (same reasoning as {@link #findByBrandAndModelKeyAndStorageGb}
     * below): Samsung's short codes are shared across storage/RAM variants, so the caller
     * must disambiguate by storageGb itself instead of a code match winning outright.
     * Ordered by id so the caller's wildcard/backfill picks (e.g. "the first candidate with
     * unknown storage") are deterministic run over run - Postgres gives no row-order
     * guarantee without an ORDER BY, and re-importing the same payload must resolve every
     * offer onto the exact same PhoneModel every time.
     */
    @Query("SELECT pm FROM PhoneModel pm WHERE LOWER(pm.brand) = LOWER(:brand) AND pm.modelCode = :modelCode " +
            "ORDER BY pm.id ASC")
    List<PhoneModel> findByBrandAndModelCode(@Param("brand") String brand, @Param("modelCode") String modelCode);

    /**
     * Tier 2 candidates: exact (brand, modelKey, storageGb), ramGb intentionally excluded
     * so the caller can apply its own wildcard/backfill rules for a null ramGb instead of
     * treating it as "distinct" (Spring Data's derived "=" binds null as never-matching,
     * which is why storageGb needs an explicit IS NULL branch here too). Ordered by id for
     * the same determinism reason as {@link #findByBrandAndModelCode}.
     */
    @Query("SELECT pm FROM PhoneModel pm " +
            "WHERE LOWER(pm.brand) = LOWER(:brand) AND pm.modelKey = :modelKey " +
            "AND ((:storageGb IS NULL AND pm.storageGb IS NULL) OR pm.storageGb = :storageGb) " +
            "ORDER BY pm.id ASC")
    List<PhoneModel> findByBrandAndModelKeyAndStorageGb(@Param("brand") String brand,
                                                         @Param("modelKey") String modelKey,
                                                         @Param("storageGb") Integer storageGb);

    /**
     * Tier 3 bucket: every model of the same brand and storageGb, to be ranked by title
     * similarity by the caller. Ordered by id so that if two candidates ever tie on
     * similarity score, the winner (first strictly-greater score wins in the caller's loop)
     * is deterministic rather than whatever order Postgres happened to return that time.
     */
    @Query("SELECT pm FROM PhoneModel pm WHERE LOWER(pm.brand) = LOWER(:brand) " +
            "AND ((:storageGb IS NULL AND pm.storageGb IS NULL) OR pm.storageGb = :storageGb) " +
            "ORDER BY pm.id ASC")
    List<PhoneModel> findByBrandAndStorageGbForFuzzyMatch(@Param("brand") String brand,
                                                           @Param("storageGb") Integer storageGb);
}
