package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.dto.PhoneDto;
import finki.ukim.mk.phone_aggregator.dto.PhoneResponseDto;
import finki.ukim.mk.phone_aggregator.model.Phone;
import finki.ukim.mk.phone_aggregator.repository.PhoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhoneService {

    private final PhoneRepository phoneRepository;

    public PhoneService(PhoneRepository phoneRepository) {
        this.phoneRepository = phoneRepository;
    }

    public long saveAllPhones(List<PhoneDto> phoneDtos) {
        List<Phone> phones = phoneDtos.stream()
                .map(this::convertDtoToEntity)
                .toList();

        phoneRepository.saveAll(phones);
        return phones.size();
    }

    private Phone convertDtoToEntity(PhoneDto dto) {
        Phone phone = new Phone();
        phone.setBrand(dto.getBrand());
        phone.setTitle(dto.getTitle());
        phone.setRawTitle(dto.getRawTitle());
        phone.setSiteLink(dto.getSiteLink());
        phone.setPrice(dto.getPrice());
        phone.setSource(dto.getSource());
        return phone;
    }

    public Page<PhoneResponseDto> getPhones(Pageable pageable) {
        Page<Phone> page = phoneRepository.findAll(pageable);
        return page.map(this::convertEntityToResponseDto);
    }

    private PhoneResponseDto convertEntityToResponseDto(Phone phone) {
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
