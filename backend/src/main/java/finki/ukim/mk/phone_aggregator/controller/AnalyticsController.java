package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.*;
import finki.ukim.mk.phone_aggregator.service.PhoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final PhoneService phoneService;

    public AnalyticsController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    /**
     * Get average price by brand
     * @return List of brands with their average prices
     */
    @GetMapping("/average-price-by-brand")
    public ResponseEntity<List<BrandAveragePriceDto>> getAveragePriceByBrand() {
        List<BrandAveragePriceDto> result = phoneService.getAveragePriceByBrand();
        return ResponseEntity.ok(result);
    }

    /**
     * Get listings count per source
     * @return List of sources with their phone counts
     */
    @GetMapping("/listings-per-source")
    public ResponseEntity<List<SourceCountDto>> getListingsPerSource() {
        List<SourceCountDto> result = phoneService.getListingsPerSource();
        return ResponseEntity.ok(result);
    }

    /**
     * Get price comparison across sources for a specific phone.
     * The parameter is kept as "normalizedTitle" for API compatibility, but is now
     * matched against PhoneModel.modelKey (built by the same normalization function
     * the old Phone.normalizedTitle column used).
     * @param normalizedTitle The normalized model key of the phone to compare
     * @return List of sources with prices for the specified phone
     */
    @GetMapping("/price-comparison")
    public ResponseEntity<List<SourcePriceDto>> getPriceComparison(
            @RequestParam String normalizedTitle
    ) {
        List<SourcePriceDto> result = phoneService.getPricesBySourceForPhone(normalizedTitle);
        return ResponseEntity.ok(result);
    }

    /**
     * Get the cheapest phone per brand
     * @return Map of brands to their cheapest phones
     */
    @GetMapping("/cheapest-per-brand")
    public ResponseEntity<Map<String, CheapestPhoneDto>> getCheapestPerBrand() {
        return ResponseEntity.ok(phoneService.getCheapestPerBrand());
    }

    /**
     * Get phone counts bucketed into price ranges
     * @return List of price ranges with their phone counts
     */
    @GetMapping("/price-distribution")
    public ResponseEntity<List<PriceDistributionDto>> getPriceDistribution() {
        List<PriceDistributionDto> result = phoneService.getPriceDistribution();
        return ResponseEntity.ok(result);
    }
}

