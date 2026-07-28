package finki.ukim.mk.phone_aggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for phone filter parameters
 * All fields are optional
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneFilterDto {

    private String search;           // Search in title and rawTitle (case-insensitive)

    private List<String> brands;     // Filter by brand names

    private List<String> sources;    // Filter by store/source names

    private Integer minPrice;        // Minimum price filter

    private Integer maxPrice;        // Maximum price filter
}

