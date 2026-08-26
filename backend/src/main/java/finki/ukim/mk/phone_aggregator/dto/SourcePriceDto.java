package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourcePriceDto {
    private String source;
    private Integer price;
}

