package finki.ukim.mk.phone_aggregator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Import payload for a single scraped listing, as POSTed to /api/phones/import.
 * Field names match phones.json exactly, including the scraper's snake_case
 * "variant_key" key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDto {

    private String brand;

    private String title;

    private String rawTitle;

    private String siteLink;

    private Integer price;

    private String imageUrl;

    @JsonProperty("variant_key")
    private String variantKey;

    private Integer ramGb;

    private Integer storageGb;

    private String colorRaw;

    private String modelCode;

    private String source;
}
