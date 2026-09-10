package finki.ukim.mk.phone_aggregator.repository;

import finki.ukim.mk.phone_aggregator.model.ScrapeRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
}
