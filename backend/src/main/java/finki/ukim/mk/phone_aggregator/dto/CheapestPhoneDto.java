package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheapestPhoneDto {
    private Long id;
    private String brand;
    private String title;
    private Integer price;
    private String imageUrl;
    private String source;
    private String siteLink;
}

