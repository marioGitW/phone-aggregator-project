package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneResponseDto {

    private Long id;

    private String brand;

    private String title;

    private String rawTitle;

    private String siteLink;

    private Integer price;

    private String source;

    private LocalDateTime createdAt;
}

