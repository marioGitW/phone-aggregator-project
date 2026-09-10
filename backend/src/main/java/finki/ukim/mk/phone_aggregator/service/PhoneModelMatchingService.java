package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.model.MatchStrategy;
import finki.ukim.mk.phone_aggregator.model.PhoneModel;
import finki.ukim.mk.phone_aggregator.repository.PhoneModelRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves which {@link PhoneModel} an incoming offer belongs to during import, via a
 * four-tier cascade, most-specific first:
 * <ol>
 *   <li>{@link MatchStrategy#CODE} - exact (brand, modelCode, storageGb), storage wildcarded when null,
 *       ramGb wildcarded when null - same rules as the structured tier. A modelCode match whose
 *       storage genuinely conflicts (no unknown-storage candidate to backfill) is not a match;
 *       it falls through to the structured tier rather than winning on the wrong variant.</li>
 *   <li>{@link MatchStrategy#STRUCTURED} - exact (brand, modelKey, storageGb), ramGb wildcarded when null.</li>
 *   <li>{@link MatchStrategy#FUZZY} - Jaro-Winkler >= {@link #FUZZY_MATCH_THRESHOLD}, restricted to same-brand/
 *       same-storageGb candidates that also agree on every {@link #extractDiscriminators(String) discriminator token}.</li>
 *   <li>{@link MatchStrategy#NEW} - nothing matched, so a new PhoneModel is created.</li>
 * </ol>
 * Whenever an offer with a known modelCode resolves onto an existing model that doesn't
 * have one yet, that model's modelCode is backfilled - see {@link #backfillModelCode}.
 */
@Service
public class PhoneModelMatchingService {

    private static final double FUZZY_MATCH_THRESHOLD = 0.90;

    /**
     * Qualifier words that change which phone a title names, even when the rest of the
     * title is nearly identical. Combined with any token containing a digit (generation/
     * series numbers: "s25", "a57", "14", "12s", "8"), this is the discriminator
     * vocabulary tested by {@link #extractDiscriminators(String)}.
     */
    private static final Set<String> DISCRIMINATOR_SUFFIXES = Set.of(
            "fe", "edge", "plus", "ultra", "pro", "max", "mini", "lite", "neo",
            "note", "flip", "fold"
    );

    private final PhoneModelRepository phoneModelRepository;
    private final StringSimilarityService stringSimilarityService;

    public PhoneModelMatchingService(PhoneModelRepository phoneModelRepository,
                                      StringSimilarityService stringSimilarityService) {
        this.phoneModelRepository = phoneModelRepository;
        this.stringSimilarityService = stringSimilarityService;
    }

    public record MatchResult(PhoneModel phoneModel, MatchStrategy strategy, double confidence) {
    }

    public MatchResult resolve(String brand, String modelKey, Integer storageGb, Integer ramGb,
                                String modelCode, String displayName) {

        MatchResult codeMatch = matchByModelCode(brand, modelCode, storageGb, ramGb);
        if (codeMatch != null) {
            return codeMatch;
        }

        MatchResult structuredMatch = matchStructured(brand, modelKey, storageGb, ramGb);
        if (structuredMatch != null) {
            backfillModelCode(structuredMatch.phoneModel(), modelCode);
            return structuredMatch;
        }

        MatchResult fuzzyMatch = matchFuzzy(brand, modelKey, storageGb);
        if (fuzzyMatch != null) {
            backfillModelCode(fuzzyMatch.phoneModel(), modelCode);
            return fuzzyMatch;
        }

        return createNew(brand, modelKey, storageGb, ramGb, modelCode, displayName);
    }

    /**
     * Tier 1: exact (brand, modelCode, storageGb). A short Samsung code like "SM-A366" is
     * shared across that phone's storage/RAM variants, so modelCode alone can't stand in
     * for the natural key the way it seemed to - storageGb has to agree too, with the same
     * wildcard/backfill treatment {@link #matchStructured} gives ramGb. If every model
     * sharing this code has a different, known storage, the code can't disambiguate at all;
     * returning null here lets the structured tier decide instead of guessing wrong.
     */
    private MatchResult matchByModelCode(String brand, String modelCode, Integer storageGb, Integer ramGb) {
        if (modelCode == null || modelCode.isBlank()) {
            return null;
        }

        List<PhoneModel> byCode = phoneModelRepository.findByBrandAndModelCode(brand, modelCode);
        if (byCode.isEmpty()) {
            return null;
        }

        List<PhoneModel> byCodeAndStorage = resolveStorageWildcard(byCode, storageGb);
        if (byCodeAndStorage == null) {
            return null;
        }

        return resolveRamWildcard(byCodeAndStorage, ramGb, MatchStrategy.CODE);
    }

    /**
     * Tier 2: exact (brand, modelKey, storageGb), with ramGb resolved via
     * {@link #resolveRamWildcard}.
     */
    private MatchResult matchStructured(String brand, String modelKey, Integer storageGb, Integer ramGb) {
        List<PhoneModel> candidates = phoneModelRepository.findByBrandAndModelKeyAndStorageGb(brand, modelKey, storageGb);
        if (candidates.isEmpty()) {
            return null;
        }

        return resolveRamWildcard(candidates, ramGb, MatchStrategy.STRUCTURED);
    }

    /**
     * Narrows a set of same-modelCode candidates to the ones agreeing on storageGb. A null
     * incoming storageGb is a wildcard - any candidate matches, preferring one whose storage
     * is already known over an "unknown storage" one. A known storageGb either matches
     * candidates exactly, backfills the first "unknown storage" candidate found, or - if
     * every candidate has a different, known storage - returns null: a genuine conflict, not
     * something this tier can resolve.
     */
    private List<PhoneModel> resolveStorageWildcard(List<PhoneModel> candidates, Integer storageGb) {
        if (storageGb == null) {
            PhoneModel preferred = candidates.stream()
                    .filter(c -> c.getStorageGb() != null)
                    .findFirst()
                    .orElseGet(() -> candidates.get(0));
            return List.of(preferred);
        }

        List<PhoneModel> exact = candidates.stream().filter(c -> storageGb.equals(c.getStorageGb())).toList();
        if (!exact.isEmpty()) {
            return exact;
        }

        for (PhoneModel candidate : candidates) {
            if (candidate.getStorageGb() == null) {
                candidate.setStorageGb(storageGb);
                phoneModelRepository.save(candidate);
                return List.of(candidate);
            }
        }

        return null;
    }

    /**
     * Resolves a set of already storage/modelKey-agreeing candidates down to one by ramGb.
     * A null incoming ramGb is a wildcard - any candidate matches, preferring one whose RAM
     * is already known. A known ramGb either matches a candidate exactly, backfills the
     * first "unknown RAM" candidate found, or - if every candidate has a different, known
     * RAM - returns null: a genuinely different variant, not a match.
     */
    private MatchResult resolveRamWildcard(List<PhoneModel> candidates, Integer ramGb, MatchStrategy strategy) {
        if (ramGb == null) {
            PhoneModel match = candidates.stream()
                    .filter(c -> c.getRamGb() != null)
                    .findFirst()
                    .orElseGet(() -> candidates.get(0));
            return new MatchResult(match, strategy, 1.0);
        }

        for (PhoneModel candidate : candidates) {
            if (ramGb.equals(candidate.getRamGb())) {
                return new MatchResult(candidate, strategy, 1.0);
            }
        }

        for (PhoneModel candidate : candidates) {
            if (candidate.getRamGb() == null) {
                candidate.setRamGb(ramGb);
                phoneModelRepository.save(candidate);
                return new MatchResult(candidate, strategy, 1.0);
            }
        }

        return null;
    }

    /**
     * Tier 3: title similarity within the (brand, storageGb) bucket - but only among
     * candidates whose discriminator tokens exactly match the incoming offer's. Jaro-
     * Winkler alone isn't safe here: its common-prefix bonus scores "samsung galaxy s25"
     * and "samsung galaxy a57" at 0.956 purely off the shared "samsung galaxy " prefix,
     * with no regard for what the prefix is followed by. The discriminator check runs
     * first and rejects candidates outright before similarity is ever computed on them.
     */
    private MatchResult matchFuzzy(String brand, String modelKey, Integer storageGb) {
        Set<String> targetDiscriminators = extractDiscriminators(modelKey);

        PhoneModel bestMatch = null;
        double bestScore = 0.0;

        for (PhoneModel candidate : phoneModelRepository.findByBrandAndStorageGbForFuzzyMatch(brand, storageGb)) {
            if (!extractDiscriminators(candidate.getModelKey()).equals(targetDiscriminators)) {
                continue;
            }

            double score = stringSimilarityService.calculateSimilarity(candidate.getModelKey(), modelKey);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch != null && bestScore >= FUZZY_MATCH_THRESHOLD) {
            return new MatchResult(bestMatch, MatchStrategy.FUZZY, bestScore);
        }

        return null;
    }

    /**
     * The tokens of a modelKey that identify *which* phone it is rather than just
     * describing it: series/generation numbers ("s25", "a57", "14", "12s", "8" - any
     * token containing a digit, since storage/RAM digits are already stripped out of
     * modelKey upstream) plus qualifier words that name a distinct model line or tier
     * ("note", "fe", "edge", "plus", "ultra", "pro", "max", "flip", "fold", ...). Two
     * modelKeys whose discriminator sets differ are never the same phone, no matter how
     * similar the rest of the title looks.
     */
    private Set<String> extractDiscriminators(String modelKey) {
        Set<String> discriminators = new HashSet<>();
        if (modelKey == null || modelKey.isBlank()) {
            return discriminators;
        }

        for (String token : modelKey.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (DISCRIMINATOR_SUFFIXES.contains(token) || containsDigit(token)) {
                discriminators.add(token);
            }
        }

        return discriminators;
    }

    private boolean containsDigit(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Populates a model's modelCode from an offer carrying one, the first time it's known. */
    private void backfillModelCode(PhoneModel model, String modelCode) {
        if (modelCode != null && !modelCode.isBlank() && model.getModelCode() == null) {
            model.setModelCode(modelCode);
            phoneModelRepository.save(model);
        }
    }

    /** Tier 4: nothing matched - this is a new phone. */
    private MatchResult createNew(String brand, String modelKey, Integer storageGb, Integer ramGb,
                                   String modelCode, String displayName) {
        PhoneModel created = new PhoneModel();
        created.setBrand(brand);
        created.setModelKey(modelKey);
        created.setStorageGb(storageGb);
        created.setRamGb(ramGb);
        created.setModelCode(modelCode);
        created.setDisplayName(displayName);
        created.setCreatedAt(LocalDateTime.now());
        phoneModelRepository.save(created);

        return new MatchResult(created, MatchStrategy.NEW, 1.0);
    }
}
