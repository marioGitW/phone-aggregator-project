package finki.ukim.mk.phone_aggregator;

import finki.ukim.mk.phone_aggregator.repository.OfferRepository;
import finki.ukim.mk.phone_aggregator.repository.PhoneModelRepository;
import finki.ukim.mk.phone_aggregator.repository.PriceSnapshotRepository;
import finki.ukim.mk.phone_aggregator.repository.ScrapeRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves POST /api/phones/import is idempotent against the real scraper output: importing
 * the same phones.json twice must not create a single new PhoneModel or Offer, and must
 * leave every offer with exactly two PriceSnapshots (one per run) - no more, no fewer.
 * <p>
 * Runs against a real, disposable Postgres (Testcontainers) rather than mocks, so the same
 * Flyway migrations, unique indexes, and matching-cascade queries that run in production run
 * here too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class PhoneImportIdempotencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PhoneModelRepository phoneModelRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private ScrapeRunRepository scrapeRunRepository;

    @Test
    void importingTheSamePhonesJsonTwiceIsIdempotent() throws IOException {
        String phonesJson = readPhonesJson();

        importAndExpectCreated(phonesJson);

        long modelCountAfterFirst = phoneModelRepository.count();
        long offerCountAfterFirst = offerRepository.count();
        long snapshotCountAfterFirst = priceSnapshotRepository.count();

        assertThat(offerCountAfterFirst)
                .as("every record in phones.json should have produced exactly one offer")
                .isEqualTo(snapshotCountAfterFirst);
        assertThat(scrapeRunRepository.count())
                .as("the first import should open and close exactly one ScrapeRun")
                .isEqualTo(1L);

        importAndExpectCreated(phonesJson);

        assertThat(phoneModelRepository.count())
                .as("re-importing the exact same payload must create zero new PhoneModels")
                .isEqualTo(modelCountAfterFirst);
        assertThat(offerRepository.count())
                .as("re-importing the exact same payload must create zero new Offers")
                .isEqualTo(offerCountAfterFirst);
        assertThat(priceSnapshotRepository.count())
                .as("every offer should have gained exactly one more snapshot")
                .isEqualTo(snapshotCountAfterFirst * 2);
        assertThat(scrapeRunRepository.count())
                .as("two imports should open and close two ScrapeRuns")
                .isEqualTo(2L);

        assertThat(priceSnapshotRepository.countGroupedByOfferId())
                .as("every single offer must have exactly two snapshots - no more, no fewer")
                .hasSize((int) offerCountAfterFirst)
                .allSatisfy(row -> assertThat((Long) row[1]).isEqualTo(2L));
    }

    private void importAndExpectCreated(String phonesJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(phonesJson, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/phones/import", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Reads the real scraper output rather than a fixture, so this test exercises the exact
     * cascade behavior the last several audits were run against. Tries both the normal
     * Maven working directory (the backend module root) and the repo root, so it isn't
     * brittle to how the test happens to be launched.
     */
    private String readPhonesJson() throws IOException {
        for (String candidate : List.of("../scraper/phones.json", "scraper/phones.json")) {
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        }
        throw new IllegalStateException(
                "Could not find scraper/phones.json relative to the test working directory: "
                        + Path.of("").toAbsolutePath());
    }
}
