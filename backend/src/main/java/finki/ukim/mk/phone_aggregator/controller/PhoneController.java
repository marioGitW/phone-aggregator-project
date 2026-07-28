package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.PhoneDto;
import finki.ukim.mk.phone_aggregator.dto.PhoneFilterDto;
import finki.ukim.mk.phone_aggregator.dto.PhoneResponseDto;
import finki.ukim.mk.phone_aggregator.service.PhoneService;
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

@RestController
@RequestMapping("/api/phones")
@CrossOrigin(origins = "*")
public class PhoneController {

    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
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
}
