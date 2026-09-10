package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.*;
import finki.ukim.mk.phone_aggregator.model.*;
import finki.ukim.mk.phone_aggregator.repository.OfferListingRepository;
import finki.ukim.mk.phone_aggregator.repository.OfferRepository;
import finki.ukim.mk.phone_aggregator.repository.PriceSnapshotRepository;
import finki.ukim.mk.phone_aggregator.repository.ScrapeRunRepository;
import finki.ukim.mk.phone_aggregator.specification.OfferListingSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PhoneService {

    private final OfferListingRepository offerListingRepository;
    private final OfferRepository offerRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ScrapeRunRepository scrapeRunRepository;
    private final PhoneModelMatchingService phoneModelMatchingService;
    private final PhoneNormalizationService phoneNormalizationService;
    private final ColorCanonicalizationService colorCanonicalizationService;

    public PhoneService(OfferListingRepository offerListingRepository,
                         OfferRepository offerRepository,
                         PriceSnapshotRepository priceSnapshotRepository,
                         ScrapeRunRepository scrapeRunRepository,
                         PhoneModelMatchingService phoneModelMatchingService,
                         PhoneNormalizationService phoneNormalizationService,
                         ColorCanonicalizationService colorCanonicalizationService) {
        this.offerListingRepository = offerListingRepository;
        this.offerRepository = offerRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.scrapeRunRepository = scrapeRunRepository;
        this.phoneModelMatchingService = phoneModelMatchingService;
        this.phoneNormalizationService = phoneNormalizationService;
        this.colorCanonicalizationService = colorCanonicalizationService;
    }

    /**
     * Imports one full scrape's worth of listings as a single ScrapeRun: each dto is
     * matched (or linked) to a PhoneModel, upserted as an Offer (identified by source +
     * variantKey/siteLink), and given a fresh PriceSnapshot. Offers not touched by this
     * run are deactivated, since main.py always POSTs every source's results in one call.
     * <p>
     * Idempotent by construction: re-posting the exact same payload finds every offer by
     * its identity key (never inserts a duplicate), finds every PhoneModel by the same
     * exact code/modelKey+storage it resolved to last time (the matching cascade's
     * candidate queries are ordered deterministically - see PhoneModelRepository - so it
     * never has to guess between ties), and always appends exactly one new PriceSnapshot
     * per offer per run. Two imports of the same payload => zero new models, zero new
     * offers, two snapshots per offer.
     */
    @Transactional
    public long saveAllPhones(List<PhoneDto> phoneDtos) {
        ScrapeRun scrapeRun = new ScrapeRun();
        scrapeRun.setStartedAt(LocalDateTime.now());
        scrapeRun.setStatus(ScrapeRunStatus.RUNNING);
        scrapeRunRepository.save(scrapeRun);

        try {
            int itemsScraped = 0;

            for (PhoneDto dto : phoneDtos) {
                importOne(dto, scrapeRun);
                itemsScraped++;
            }

            offerRepository.deactivateStaleOffers(scrapeRun.getStartedAt());

            scrapeRun.setFinishedAt(LocalDateTime.now());
            scrapeRun.setStatus(ScrapeRunStatus.COMPLETED);
            scrapeRun.setItemsScraped(itemsScraped);
            scrapeRunRepository.save(scrapeRun);

            return itemsScraped;
        } catch (RuntimeException e) {
            scrapeRun.setFinishedAt(LocalDateTime.now());
            scrapeRun.setStatus(ScrapeRunStatus.FAILED);
            scrapeRunRepository.save(scrapeRun);
            throw e;
        }
    }

    private void importOne(PhoneDto dto, ScrapeRun scrapeRun) {
        String titleForKey = dto.getRawTitle() != null && !dto.getRawTitle().isBlank()
                ? dto.getRawTitle()
                : dto.getTitle();
        String modelKey = phoneNormalizationService.normalizeTitle(titleForKey);

        PhoneModelMatchingService.MatchResult match = phoneModelMatchingService.resolve(
                dto.getBrand(), modelKey, dto.getStorageGb(), dto.getRamGb(), dto.getModelCode(), dto.getTitle());

        LocalDateTime now = LocalDateTime.now();
        Offer offer = offerRepository.findBySourceAndIdentityKey(dto.getSource(), dto.getVariantKey(), dto.getSiteLink())
                .orElseGet(Offer::new);
        boolean isNewOffer = offer.getId() == null;

        offer.setPhoneModel(match.phoneModel());
        offer.setSource(dto.getSource());
        offer.setSiteLink(dto.getSiteLink());
        offer.setVariantKey(dto.getVariantKey());
        offer.setRawTitle(dto.getRawTitle());
        offer.setColorRaw(dto.getColorRaw());
        offer.setColorCanonical(colorCanonicalizationService.canonicalize(dto.getColorRaw()));
        offer.setImageUrl(dto.getImageUrl());
        offer.setMatchStrategy(match.strategy());
        offer.setMatchConfidence(match.confidence());
        offer.setLastSeenAt(now);
        offer.setIsActive(true);
        if (isNewOffer) {
            offer.setFirstSeenAt(now);
        }
        offerRepository.save(offer);

        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setOffer(offer);
        snapshot.setScrapeRun(scrapeRun);
        snapshot.setPrice(dto.getPrice());
        snapshot.setScrapedAt(now);
        priceSnapshotRepository.save(snapshot);
    }

    /**
     * Get phones with optional filtering
     * @param filters Optional filter parameters (can be null or have null fields)
     * @param pageable Pagination and sorting parameters
     * @return Page of PhoneResponseDto matching the criteria
     */
    public Page<PhoneResponseDto> getPhones(PhoneFilterDto filters, Pageable pageable) {
        Specification<OfferListing> specification = OfferListingSpecifications.fromFilter(filters);
        Page<OfferListing> page = offerListingRepository.findAll(specification, pageable);
        return page.map(this::convertToResponseDto);
    }

    /**
     * Get all phones without filtering (backward compatible)
     */
    public Page<PhoneResponseDto> getPhones(Pageable pageable) {
        return getPhones(null, pageable);
    }

    private PhoneResponseDto convertToResponseDto(OfferListing listing) {
        return new PhoneResponseDto(
                listing.getId(),
                listing.getBrand(),
                listing.getTitle(),
                listing.getRawTitle(),
                listing.getSiteLink(),
                listing.getPrice(),
                listing.getImageUrl(),
                listing.getSource(),
                listing.getCreatedAt()
        );
    }

    /**
     * Get all distinct brands with at least one active listing, sorted alphabetically
     */
    public List<String> getAllBrands() {
        return offerListingRepository.findDistinctBrands();
    }

    /**
     * Get all distinct sources (stores) with at least one active listing, sorted alphabetically
     */
    public List<String> getAllSources() {
        return offerListingRepository.findDistinctSources();
    }

    /**
     * Other active listings of the same phone model as the given offer, excluding itself.
     * Empty Optional means no active offer exists with that id.
     */
    public Optional<List<PhoneResponseDto>> findSimilarPhones(Long offerId) {
        return offerListingRepository.findById(offerId)
                .map(listing -> offerListingRepository.findByPhoneModelIdAndIdNot(listing.getPhoneModelId(), offerId)
                        .stream()
                        .map(this::convertToResponseDto)
                        .toList());
    }

    // Analytics methods
    public List<BrandAveragePriceDto> getAveragePriceByBrand() {
        return offerRepository.getAveragePriceByBrandRaw().stream()
                .map(row -> new BrandAveragePriceDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public List<SourceCountDto> getListingsPerSource() {
        return offerRepository.getListingsPerSource();
    }

    public List<SourcePriceDto> getPricesBySourceForPhone(String normalizedTitle) {
        return offerRepository.getPricesBySourceForModelKeyRaw(normalizedTitle).stream()
                .map(row -> new SourcePriceDto((String) row[0], ((Number) row[1]).intValue()))
                .toList();
    }

    /**
     * The cheapest active offer per brand, computed in one pass (replaces the previous
     * per-brand query + loop, which re-ran the same ranking query once per brand).
     */
    public Map<String, CheapestPhoneDto> getCheapestPerBrand() {
        Map<String, CheapestPhoneDto> result = new HashMap<>();
        for (Object[] row : offerRepository.getCheapestOfferByBrandRaw()) {
            String brand = (String) row[1];
            result.put(brand, new CheapestPhoneDto(
                    ((Number) row[0]).longValue(),
                    brand,
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    (String) row[4],
                    (String) row[5],
                    (String) row[6]
            ));
        }
        return result;
    }

    public List<PriceDistributionDto> getPriceDistribution() {
        return offerRepository.getPriceDistributionRaw().stream()
                .map(row -> new PriceDistributionDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
