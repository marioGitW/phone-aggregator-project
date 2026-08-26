package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.*;
import finki.ukim.mk.phone_aggregator.service.PhoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
     * Get price comparison across sources for a specific phone
     * @param normalizedTitle The normalized title of the phone to compare
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
        List<String> brands = phoneService.getAllBrands();
        Map<String, CheapestPhoneDto> result = new HashMap<>();

        for (String brand : brands) {
            List<CheapestPhoneDto> cheapest = phoneService.getCheapestPhoneByBrand(brand);
            if (!cheapest.isEmpty()) {
                result.put(brand, cheapest.get(0));
            }
        }

        return ResponseEntity.ok(result);
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

