package finki.ukim.mk.phone_aggregator.model;

import java.util.Locale;

/**
 * How an incoming offer was linked to its {@link PhoneModel} during import, in the order
 * the matching cascade tries them. Persisted (via {@link MatchStrategyConverter}) as the
 * lowercase {@link #dbValue()} - "code" | "structured" | "fuzzy" | "new".
 */
public enum MatchStrategy {
    /** Exact match on modelCode (e.g. a Samsung SM- code). Deterministic; short-circuits the rest of the cascade. */
    CODE,
    /** Exact match on (brand, modelKey, storageGb), with ramGb treated as a wildcard when null. */
    STRUCTURED,
    /** No exact match; linked to the closest model in the same (brand, storageGb) bucket by title similarity. */
    FUZZY,
    /** No existing model matched, so a new one was created for this offer. */
    NEW;

    public String dbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MatchStrategy fromDbValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
