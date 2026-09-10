package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link MatchStrategy} as its lowercase {@link MatchStrategy#dbValue()} instead
 * of the enum constant name, so the stored value is "code"/"structured"/"fuzzy"/"new".
 */
@Converter(autoApply = true)
public class MatchStrategyConverter implements AttributeConverter<MatchStrategy, String> {

    @Override
    public String convertToDatabaseColumn(MatchStrategy attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public MatchStrategy convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MatchStrategy.fromDbValue(dbData);
    }
}
