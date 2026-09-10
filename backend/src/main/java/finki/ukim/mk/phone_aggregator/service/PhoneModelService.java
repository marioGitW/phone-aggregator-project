package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.PricePointDto;
import finki.ukim.mk.phone_aggregator.dto.SourcePriceHistoryDto;
import finki.ukim.mk.phone_aggregator.repository.PhoneModelRepository;
import finki.ukim.mk.phone_aggregator.repository.PriceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Read-side queries about a canonical {@link finki.ukim.mk.phone_aggregator.model.PhoneModel}. */
@Service
public class PhoneModelService {

    private final PhoneModelRepository phoneModelRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public PhoneModelService(PhoneModelRepository phoneModelRepository,
                              PriceSnapshotRepository priceSnapshotRepository) {
        this.phoneModelRepository = phoneModelRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    /**
     * Every price ever recorded for this phone model, grouped by source and ordered by
     * time - one series per store, for a price-over-time chart.
     */
    public List<SourcePriceHistoryDto> getPriceHistory(Long phoneModelId) {
        if (!phoneModelRepository.existsById(phoneModelId)) {
            throw new ResponseStatusException(NOT_FOUND, "Phone model not found");
        }

        Map<String, List<PricePointDto>> bySource = new LinkedHashMap<>();
        for (Object[] row : priceSnapshotRepository.findPriceHistoryByPhoneModelId(phoneModelId)) {
            String source = (String) row[0];
            LocalDateTime scrapedAt = (LocalDateTime) row[1];
            Integer price = (Integer) row[2];
            bySource.computeIfAbsent(source, s -> new ArrayList<>()).add(new PricePointDto(scrapedAt, price));
        }

        return bySource.entrySet().stream()
                .map(e -> new SourcePriceHistoryDto(e.getKey(), e.getValue()))
                .toList();
    }
}
