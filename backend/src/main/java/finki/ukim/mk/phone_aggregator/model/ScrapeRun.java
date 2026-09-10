package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One execution of the scraper's import (a single POST to /api/phones/import), grouping
 * every {@link PriceSnapshot} it produced.
 */
@Entity
@Table(name = "scrape_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScrapeRunStatus status;

    @Column(name = "items_scraped")
    private Integer itemsScraped;
}
