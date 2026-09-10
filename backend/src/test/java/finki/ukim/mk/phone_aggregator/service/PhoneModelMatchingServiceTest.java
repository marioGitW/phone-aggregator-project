package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.model.MatchStrategy;
import finki.ukim.mk.phone_aggregator.model.PhoneModel;
import finki.ukim.mk.phone_aggregator.repository.PhoneModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the two matching-cascade fixes: modelCode backfill on an existing model, and the
 * discriminator guard that must reject a fuzzy match before Jaro-Winkler similarity is
 * even computed, regardless of how high that similarity would otherwise score.
 */
class PhoneModelMatchingServiceTest {

    private PhoneModelRepository phoneModelRepository;
    private PhoneModelMatchingService service;

    @BeforeEach
    void setUp() {
        phoneModelRepository = mock(PhoneModelRepository.class);
        service = new PhoneModelMatchingService(phoneModelRepository, new StringSimilarityService());

        // Tier 1/2 never match unless a test stubs them otherwise, so every scenario below
        // exercises tier 3 (fuzzy) and tier 4 (new) only.
        when(phoneModelRepository.findByBrandAndModelCode(anyString(), anyString())).thenReturn(List.of());
        when(phoneModelRepository.findByBrandAndModelKeyAndStorageGb(anyString(), anyString(), any())).thenReturn(List.of());
    }

    private static PhoneModel model(long id, String brand, String modelKey, Integer storageGb, Integer ramGb, String modelCode) {
        PhoneModel model = new PhoneModel();
        model.setId(id);
        model.setBrand(brand);
        model.setModelKey(modelKey);
        model.setStorageGb(storageGb);
        model.setRamGb(ramGb);
        model.setModelCode(modelCode);
        model.setDisplayName(modelKey);
        model.setCreatedAt(LocalDateTime.now());
        return model;
    }

    static Stream<Arguments> mustNotMatch() {
        return Stream.of(
                Arguments.of("samsung", "samsung galaxy s25", "samsung galaxy s26"),
                Arguments.of("samsung", "samsung galaxy s25", "samsung galaxy s25 fe"),
                Arguments.of("samsung", "samsung galaxy s25", "samsung galaxy a57"),
                Arguments.of("xiaomi", "xiaomi redmi note 14 pro plus", "xiaomi redmi note 15"),
                Arguments.of("xiaomi", "xiaomi redmi note 14", "xiaomi redmi note 12s"),
                Arguments.of("samsung", "samsung galaxy s26", "samsung galaxy s26 plus")
        );
    }

    @ParameterizedTest(name = "\"{1}\" vs \"{2}\" must not match")
    @MethodSource("mustNotMatch")
    void differentPhonesAreNeverFuzzyMatched(String brand, String existingModelKey, String incomingModelKey) {
        PhoneModel existing = model(1L, brand, existingModelKey, 256, 12, null);
        when(phoneModelRepository.findByBrandAndStorageGbForFuzzyMatch(brand, 256)).thenReturn(List.of(existing));
        when(phoneModelRepository.save(any(PhoneModel.class))).thenAnswer(inv -> inv.getArgument(0));

        PhoneModelMatchingService.MatchResult result =
                service.resolve(brand, incomingModelKey, 256, 12, null, incomingModelKey);

        assertThat(result.strategy())
                .as("\"%s\" must not resolve onto existing model \"%s\"", incomingModelKey, existingModelKey)
                .isEqualTo(MatchStrategy.NEW);
        assertThat(result.phoneModel().getId()).isNotEqualTo(existing.getId());
    }

    @Test
    void sameDiscriminatorsWithNoisyTitleStillFuzzyMatches() {
        PhoneModel existing = model(1L, "xiaomi", "xiaomi redmi note 14 pro", 256, 8, null);
        when(phoneModelRepository.findByBrandAndStorageGbForFuzzyMatch("xiaomi", 256)).thenReturn(List.of(existing));

        // Same discriminators {note, 14, pro} as the existing model, just one trailing
        // filler word from a differently-formatted title - the guard should let this through.
        PhoneModelMatchingService.MatchResult result = service.resolve(
                "xiaomi", "xiaomi redmi note 14 pro telefon", 256, 8, null, "irrelevant");

        assertThat(result.strategy()).isEqualTo(MatchStrategy.FUZZY);
        assertThat(result.phoneModel()).isSameAs(existing);
    }

    @Test
    void structuredMatchBackfillsMissingModelCode() {
        PhoneModel existing = model(1L, "samsung", "samsung galaxy a57", 256, 8, null);
        when(phoneModelRepository.findByBrandAndModelKeyAndStorageGb("samsung", "samsung galaxy a57", 256))
                .thenReturn(List.of(existing));

        PhoneModelMatchingService.MatchResult result =
                service.resolve("samsung", "samsung galaxy a57", 256, 8, "SM-A576BZVBEUC", "Galaxy A57");

        assertThat(result.strategy()).isEqualTo(MatchStrategy.STRUCTURED);
        assertThat(result.phoneModel().getModelCode()).isEqualTo("SM-A576BZVBEUC");
    }

    @Test
    void plusSymbolAndSpelledOutPlusResolveToTheSameModel() {
        PhoneNormalizationService normalizationService = new PhoneNormalizationService();
        String keyFromSymbol = normalizationService.normalizeTitle("Samsung Galaxy S26+ 5g 12gb/256gb black");
        String keyFromWord = normalizationService.normalizeTitle("Samsung Galaxy S26 Plus 5g 12gb/256gb black");

        // The normalization fix means these must already be identical before either offer
        // is resolved - if this fails, the two offers below aren't actually testing the
        // "MUST match" case any more.
        assertThat(keyFromSymbol).isEqualTo(keyFromWord);

        when(phoneModelRepository.save(any(PhoneModel.class))).thenAnswer(inv -> inv.getArgument(0));

        // First offer ("S26+") creates the model.
        PhoneModelMatchingService.MatchResult first =
                service.resolve("samsung", keyFromSymbol, 256, 12, null, "Galaxy S26+");
        assertThat(first.strategy()).isEqualTo(MatchStrategy.NEW);

        // Second offer, described as "S26 Plus", must land on that same model structurally.
        when(phoneModelRepository.findByBrandAndModelKeyAndStorageGb("samsung", keyFromWord, 256))
                .thenReturn(List.of(first.phoneModel()));
        PhoneModelMatchingService.MatchResult second =
                service.resolve("samsung", keyFromWord, 256, 12, null, "Galaxy S26 Plus");

        assertThat(second.strategy()).isEqualTo(MatchStrategy.STRUCTURED);
        assertThat(second.phoneModel()).isSameAs(first.phoneModel());
    }

    @Test
    void subsequentOfferWithSameCodeAndSameStorageThenMatchesByCode() {
        PhoneModel existing = model(1L, "samsung", "samsung galaxy a57", 256, 8, "SM-A576BZVBEUC");
        when(phoneModelRepository.findByBrandAndModelCode("samsung", "SM-A576BZVBEUC"))
                .thenReturn(List.of(existing));

        PhoneModelMatchingService.MatchResult result =
                service.resolve("samsung", "samsung galaxy a57 5g", 256, 8, "SM-A576BZVBEUC", "Galaxy A57 5G");

        assertThat(result.strategy()).isEqualTo(MatchStrategy.CODE);
        assertThat(result.phoneModel()).isSameAs(existing);
    }

    /**
     * The real bug from the audit: setec exposes only the short SM-A366 code (shared across
     * every Galaxy A36 storage/RAM variant) instead of a full per-SKU code. Two genuinely
     * different offers - one 6GB/128GB, one 8GB/256GB - carrying that same short code must
     * NOT collapse into one PhoneModel just because the code matches.
     */
    @Test
    void sameModelCodeButDifferentStorageResolvesToDifferentPhoneModels() {
        PhoneModel existing128 = model(1L, "samsung", "samsung galaxy a36", 128, 6, "SM-A366");
        when(phoneModelRepository.findByBrandAndModelCode("samsung", "SM-A366")).thenReturn(List.of(existing128));
        // No structured or fuzzy home either, matching the real case where the "correct"
        // 256GB model hadn't been created yet by the time this offer is processed.
        when(phoneModelRepository.findByBrandAndModelKeyAndStorageGb(anyString(), anyString(), any())).thenReturn(List.of());
        when(phoneModelRepository.findByBrandAndStorageGbForFuzzyMatch(anyString(), any())).thenReturn(List.of());
        when(phoneModelRepository.save(any(PhoneModel.class))).thenAnswer(inv -> inv.getArgument(0));

        PhoneModelMatchingService.MatchResult result = service.resolve(
                "samsung", "samsung galaxy a36", 256, 8, "SM-A366", "Galaxy A36 awesome black 8/256gb");

        assertThat(result.strategy())
                .as("a 256GB offer sharing SM-A366 with an existing 128GB model must not code-match it")
                .isEqualTo(MatchStrategy.NEW);
        assertThat(result.phoneModel().getId()).isNotEqualTo(existing128.getId());
        assertThat(result.phoneModel().getStorageGb()).isEqualTo(256);
    }

    @Test
    void codeMatchBackfillsUnknownStorage() {
        // Storage couldn't be extracted when this model was first created.
        PhoneModel existing = model(1L, "samsung", "samsung galaxy a36", null, 6, "SM-A366");
        when(phoneModelRepository.findByBrandAndModelCode("samsung", "SM-A366")).thenReturn(List.of(existing));
        when(phoneModelRepository.save(any(PhoneModel.class))).thenAnswer(inv -> inv.getArgument(0));

        PhoneModelMatchingService.MatchResult result =
                service.resolve("samsung", "samsung galaxy a36", 128, 6, "SM-A366", "Galaxy A36 128gb");

        assertThat(result.strategy()).isEqualTo(MatchStrategy.CODE);
        assertThat(result.phoneModel()).isSameAs(existing);
        assertThat(result.phoneModel().getStorageGb()).isEqualTo(128);
    }

    @Test
    void codeMatchWithNullStorageIsAWildcard() {
        PhoneModel existing = model(1L, "samsung", "samsung galaxy a36", 128, 6, "SM-A366");
        when(phoneModelRepository.findByBrandAndModelCode("samsung", "SM-A366")).thenReturn(List.of(existing));

        // ledikom-style listing: no storage figure in the raw title at all.
        PhoneModelMatchingService.MatchResult result =
                service.resolve("samsung", "samsung galaxy a36", null, null, "SM-A366", "Galaxy A36");

        assertThat(result.strategy()).isEqualTo(MatchStrategy.CODE);
        assertThat(result.phoneModel()).isSameAs(existing);
    }
}
