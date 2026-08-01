package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.dto.PhoneResponseDto;
import finki.ukim.mk.phone_aggregator.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}/offers")
    public ResponseEntity<List<PhoneResponseDto>> getOffers(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getOffers(id));
    }
}

