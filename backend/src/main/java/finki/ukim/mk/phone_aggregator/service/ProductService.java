package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.PhoneResponseDto;
import finki.ukim.mk.phone_aggregator.model.Phone;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProductService {

    private final PhoneService phoneService;
    private final PhoneSimilarityService phoneSimilarityService;

    public ProductService(PhoneService phoneService, PhoneSimilarityService phoneSimilarityService) {
        this.phoneService = phoneService;
        this.phoneSimilarityService = phoneSimilarityService;
    }

    public List<PhoneResponseDto> getOffers(Long id) {
        Phone basePhone = phoneService.findPhoneById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Phone not found"));

        return Stream.concat(Stream.of(basePhone), phoneSimilarityService.findSimilarPhones(basePhone).stream())
                .distinct()
                .map(this::convertToDto)
                .toList();
    }

    private PhoneResponseDto convertToDto(Phone phone) {
        return new PhoneResponseDto(
                phone.getId(),
                phone.getBrand(),
                phone.getTitle(),
                phone.getRawTitle(),
                phone.getSiteLink(),
                phone.getPrice(),
                phone.getSource(),
                phone.getCreatedAt()
        );
    }
}


