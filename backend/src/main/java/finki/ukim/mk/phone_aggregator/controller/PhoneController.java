package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.PhoneDto;
import finki.ukim.mk.phone_aggregator.dto.PhoneFilterDto;
import finki.ukim.mk.phone_aggregator.dto.PhoneResponseDto;
import finki.ukim.mk.phone_aggregator.model.Phone;
import finki.ukim.mk.phone_aggregator.service.PhoneService;
import finki.ukim.mk.phone_aggregator.service.PhoneSimilarityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/phones")
@CrossOrigin(origins = "*")
public class PhoneController {

    private final PhoneService phoneService;
    private final PhoneSimilarityService phoneSimilarityService;

    public PhoneController(PhoneService phoneService, PhoneSimilarityService phoneSimilarityService) {
        this.phoneService = phoneService;
        this.phoneSimilarityService = phoneSimilarityService;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importPhones(@RequestBody List<PhoneDto> phones) {
        long savedCount = phoneService.saveAllPhones(phones);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Phones imported successfully");
        response.put("saved", savedCount);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PhoneResponseDto>> getPhones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false) List<String> source,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice
    ) {
        // Build sort object
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                sortObj = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
            } else {
                sortObj = Sort.by(sort);
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        // Build filter DTO
        PhoneFilterDto filters = new PhoneFilterDto(
                search,
                brand,
                source,
                minPrice,
                maxPrice
        );

        Page<PhoneResponseDto> result = phoneService.getPhones(filters, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getBrands() {
        List<String> brands = phoneService.getAllBrands();
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/sources")
    public ResponseEntity<List<String>> getSources() {
        List<String> sources = phoneService.getAllSources();
        return ResponseEntity.ok(sources);
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<PhoneResponseDto>> getSimilarPhones(@PathVariable Long id) {
        Optional<Phone> phoneOpt = phoneService.findPhoneById(id);

        if (phoneOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<PhoneResponseDto> result = phoneSimilarityService.findSimilarPhones(phoneOpt.get())
                .stream()
                .map(phone -> new PhoneResponseDto(
                        phone.getId(),
                        phone.getBrand(),
                        phone.getTitle(),
                        phone.getRawTitle(),
                        phone.getSiteLink(),
                        phone.getPrice(),
                        phone.getSource(),
                        phone.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
}
