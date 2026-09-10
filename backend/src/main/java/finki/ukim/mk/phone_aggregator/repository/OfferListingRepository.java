package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.OfferListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferListingRepository extends JpaRepository<OfferListing, Long>, JpaSpecificationExecutor<OfferListing> {

    @Query("SELECT DISTINCT o.brand FROM OfferListing o ORDER BY o.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT o.source FROM OfferListing o ORDER BY o.source")
    List<String> findDistinctSources();

    @Query("SELECT DISTINCT o.colorCanonical FROM OfferListing o WHERE o.colorCanonical IS NOT NULL ORDER BY o.colorCanonical")
    List<String> findDistinctColors();

    @Query("SELECT DISTINCT o.storageGb FROM OfferListing o WHERE o.storageGb IS NOT NULL ORDER BY o.storageGb")
    List<Integer> findDistinctStorageOptions();

    @Query("SELECT DISTINCT o.ramGb FROM OfferListing o WHERE o.ramGb IS NOT NULL ORDER BY o.ramGb")
    List<Integer> findDistinctRamOptions();

    List<OfferListing> findByPhoneModelId(Long phoneModelId);

    List<OfferListing> findByPhoneModelIdAndIdNot(Long phoneModelId, Long excludedId);
}
