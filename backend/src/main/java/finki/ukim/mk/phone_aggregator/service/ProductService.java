package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.ProductOfferDto;
import finki.ukim.mk.phone_aggregator.model.Phone;
import finki.ukim.mk.phone_aggregator.repository.PhoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final PhoneRepository phoneRepository;
    private final PhoneNormalizationService phoneNormalizationService;

    public ProductService(PhoneRepository phoneRepository, PhoneNormalizationService phoneNormalizationService) {
        this.phoneRepository = phoneRepository;
        this.phoneNormalizationService = phoneNormalizationService;
    }

    public List<ProductOfferDto> getOffers(String normalizedTitle) {
        String normalizedQuery = phoneNormalizationService.normalizeTitle(normalizedTitle);

        return phoneRepository.findByNormalizedTitle(normalizedQuery).stream()
                .map(this::convertToDto)
                .toList();
    }

    private ProductOfferDto convertToDto(Phone phone) {
        return new ProductOfferDto(
                phone.getId(),
                phone.getBrand(),
                phone.getTitle(),
                phone.getPrice(),
                phone.getSource(),
                phone.getSiteLink(),
                phone.getCreatedAt()
        );
    }
}


