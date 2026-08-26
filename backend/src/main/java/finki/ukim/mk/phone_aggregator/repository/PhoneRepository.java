package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.dto.*;
import finki.ukim.mk.phone_aggregator.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Long>, JpaSpecificationExecutor<Phone> {

	List<Phone> findByNormalizedTitle(String normalizedTitle);

	@Query("SELECT DISTINCT p.brand FROM Phone p ORDER BY p.brand")
	List<String> findDistinctBrands();

	@Query("SELECT DISTINCT p.source FROM Phone p ORDER BY p.source")
	List<String> findDistinctSources();

	// Analytics queries
	@Query("SELECT new finki.ukim.mk.phone_aggregator.dto.BrandAveragePriceDto(p.brand, CAST(AVG(p.price) AS long)) " +
			"FROM Phone p GROUP BY p.brand ORDER BY p.brand")
	List<BrandAveragePriceDto> getAveragePriceByBrand();

	@Query("SELECT new finki.ukim.mk.phone_aggregator.dto.SourceCountDto(p.source, COUNT(p.id)) " +
			"FROM Phone p GROUP BY p.source ORDER BY p.source")
	List<SourceCountDto> getListingsPerSource();

	@Query("SELECT new finki.ukim.mk.phone_aggregator.dto.SourcePriceDto(p.source, p.price) " +
			"FROM Phone p WHERE p.normalizedTitle = :normalizedTitle ORDER BY p.source")
	List<SourcePriceDto> getPricesBySourceForPhone(String normalizedTitle);

	@Query("SELECT new finki.ukim.mk.phone_aggregator.dto.CheapestPhoneDto(" +
			"p.id, p.brand, p.title, p.price, p.imageUrl, p.source, p.siteLink) " +
			"FROM Phone p WHERE p.brand = :brand AND p.price = " +
			"(SELECT MIN(p2.price) FROM Phone p2 WHERE p2.brand = :brand)")
	List<CheapestPhoneDto> getCheapestPhoneByBrand(String brand);

	@Query("SELECT new finki.ukim.mk.phone_aggregator.dto.PriceDistributionDto(" +
			"CASE " +
			"  WHEN p.price < 10000 THEN '0-10k' " +
			"  WHEN p.price < 20000 THEN '10k-20k' " +
			"  WHEN p.price < 30000 THEN '20k-30k' " +
			"  ELSE '30k+' " +
			"END, COUNT(p.id)) " +
			"FROM Phone p " +
			"GROUP BY " +
			"CASE " +
			"  WHEN p.price < 10000 THEN '0-10k' " +
			"  WHEN p.price < 20000 THEN '10k-20k' " +
			"  WHEN p.price < 30000 THEN '20k-30k' " +
			"  ELSE '30k+' " +
			"END, " +
			"CASE " +
			"  WHEN p.price < 10000 THEN 1 " +
			"  WHEN p.price < 20000 THEN 2 " +
			"  WHEN p.price < 30000 THEN 3 " +
			"  ELSE 4 " +
			"END " +
			"ORDER BY " +
			"CASE " +
			"  WHEN p.price < 10000 THEN 1 " +
			"  WHEN p.price < 20000 THEN 2 " +
			"  WHEN p.price < 30000 THEN 3 " +
			"  ELSE 4 " +
			"END")
	List<PriceDistributionDto> getPriceDistribution();
}
