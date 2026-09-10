package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.SourcePriceHistoryDto;
import finki.ukim.mk.phone_aggregator.service.PhoneModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class PhoneModelController {

    private final PhoneModelService phoneModelService;

    public PhoneModelController(PhoneModelService phoneModelService) {
        this.phoneModelService = phoneModelService;
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<SourcePriceHistoryDto>> getPriceHistory(@PathVariable Long id) {
        return ResponseEntity.ok(phoneModelService.getPriceHistory(id));
    }
}
