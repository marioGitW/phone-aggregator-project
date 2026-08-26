package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceCountDto {
    private String source;
    private Long count;
}

