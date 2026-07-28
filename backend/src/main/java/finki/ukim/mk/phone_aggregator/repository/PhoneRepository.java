package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Long>, JpaSpecificationExecutor<Phone> {

	@Query("SELECT DISTINCT p.brand FROM Phone p ORDER BY p.brand")
	List<String> findDistinctBrands();

	@Query("SELECT DISTINCT p.source FROM Phone p ORDER BY p.source")
	List<String> findDistinctSources();
}
