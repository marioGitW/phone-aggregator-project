package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDto {

    private String brand;

    private String title;

    private String rawTitle;

    private String siteLink;

    private Integer price;

    private String source;
}

