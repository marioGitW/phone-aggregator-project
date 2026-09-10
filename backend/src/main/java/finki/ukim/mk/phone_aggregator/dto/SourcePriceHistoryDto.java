package finki.ukim.mk.phone_aggregator.dto;

import java.util.List;

/** One source's full price history for a phone model - one line on the chart. */
public record SourcePriceHistoryDto(String source, List<PricePointDto> points) {
}
