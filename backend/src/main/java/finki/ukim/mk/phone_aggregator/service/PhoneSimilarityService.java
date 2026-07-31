package finki.ukim.mk.phone_aggregator.service;

import finki.ukim.mk.phone_aggregator.model.Phone;
import finki.ukim.mk.phone_aggregator.repository.PhoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class PhoneSimilarityService {

    private static final double SIMILARITY_THRESHOLD = 0.85d;
    private static final int MAX_PREFIX_LENGTH = 4;
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MODEL_CODE_PATTERN = Pattern.compile("\\bSM-[A-Z0-9]+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORAGE_PATTERN = Pattern.compile(
            "(?i)" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*/\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*\\+\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*ram\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\b|" +
                    "\\b\\d+\\s*/\\s*\\d+\\s*(?:gb|tb|mb)?\\b"
    );
    private static final Pattern NETWORK_PATTERN = Pattern.compile("(?i)\\b(?:5g|4g|lte|dual\\s+sim|sim)\\b");

    private static final List<Pattern> COLOR_PATTERNS = List.of(
            wordPattern("black"),
            wordPattern("white"),
            wordPattern("blue"),
            wordPattern("green"),
            wordPattern("gray"),
            wordPattern("grey"),
            wordPattern("silver"),
            wordPattern("gold"),
            wordPattern("purple"),
            wordPattern("pink"),
            wordPattern("red"),
            wordPattern("yellow"),
            wordPattern("orange"),
            wordPattern("midnight"),
            wordPattern("starlight"),
            wordPattern("space gray"),
            wordPattern("space grey"),
            wordPattern("awesome olive"),
            wordPattern("awesome graphite"),
            wordPattern("light blue"),
            wordPattern("dark blue")
    );

    private static final List<String> BRAND_WORDS = List.of(
            "samsung",
            "galaxy",
            "xiaomi",
            "apple",
            "iphone",
            "redmi",
            "poco",
            "mi",
            "galaxy",
            "oppo",
            "vivo",
            "realme",
            "honor",
            "huawei",
            "motorola",
            "oneplus",
            "nokia",
            "sony",
            "google",
            "pixel",
            "sm"
    );

    private static final List<String> MODEL_SUFFIX_WORDS = List.of(
            "pro",
            "plus",
            "ultra",
            "max",
            "mini",
            "fe",
            "lite",
            "neo",
            "s",
            "a",
            "c"
    );

    private final PhoneNormalizationService phoneNormalizationService;
    private final PhoneRepository phoneRepository;

    public PhoneSimilarityService(PhoneNormalizationService phoneNormalizationService,
                                  PhoneRepository phoneRepository) {
        this.phoneNormalizationService = phoneNormalizationService;
        this.phoneRepository = phoneRepository;
    }

    public double calculateSimilarity(String title1, String title2) {
        String normalized1 = normalizeForSimilarity(title1);
        String normalized2 = normalizeForSimilarity(title2);

        if (normalized1.isBlank() || normalized2.isBlank()) {
            return 0.0d;
        }

        return jaroWinklerSimilarity(normalized1, normalized2);
    }

    public boolean isSamePhone(Phone phone1, Phone phone2) {
        if (phone1 == null || phone2 == null) {
            return false;
        }

        if (phone1.getBrand() == null || phone2.getBrand() == null) {
            return false;
        }

        if (!phone1.getBrand().trim().equalsIgnoreCase(phone2.getBrand().trim())) {
            return false;
        }

        String model1 = extractImportantModelIdentifier(phone1);
        String model2 = extractImportantModelIdentifier(phone2);

        if (model1.isBlank() || model2.isBlank() || !model1.equalsIgnoreCase(model2)) {
            return false;
        }

        return calculateSimilarity(phone1.getNormalizedTitle(), phone2.getNormalizedTitle()) >= SIMILARITY_THRESHOLD;
    }

    public List<Phone> findSimilarPhones(Phone phone) {
        if (phone == null) {
            return List.of();
        }

        return phoneRepository.findAll().stream()
                .filter(candidate -> candidate.getId() == null || !candidate.getId().equals(phone.getId()))
                .filter(candidate -> isSamePhone(phone, candidate))
                .toList();
    }

    private String extractImportantModelIdentifier(Phone phone) {
        if (phone == null) {
            return "";
        }

        String normalizedTitle = phone.getNormalizedTitle();
        if (normalizedTitle == null || normalizedTitle.isBlank()) {
            normalizedTitle = phoneNormalizationService.normalizeTitle(phone.getRawTitle() != null ? phone.getRawTitle() : phone.getTitle());
        }

        if (normalizedTitle == null || normalizedTitle.isBlank()) {
            return "";
        }

        String value = normalizedTitle.toLowerCase(Locale.ROOT).trim();
        for (String brandWord : BRAND_WORDS) {
            value = value.replaceAll("\\b" + Pattern.quote(brandWord) + "\\b", " ");
        }

        value = value.replaceAll("\\b(note|pro|plus|ultra|max|mini|fe|lite|neo|ultra\\+|pro\\+)\\b", " ");
        value = value.replaceAll("\\bsm-[a-z0-9]+\\b", " ");
        value = value.replaceAll("\\s+", " ").trim();

        String[] tokens = value.split(" ");
        StringBuilder importantModel = new StringBuilder();

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            if (token.matches("\\d+[a-z]*")) {
                importantModel.append(token).append(' ');
                continue;
            }

            if (MODEL_SUFFIX_WORDS.contains(token)) {
                importantModel.append(token).append(' ');
                continue;
            }

            if (!importantModel.isEmpty()) {
                break;
            }
        }

        if (importantModel.isEmpty()) {
            for (String token : tokens) {
                if (token.matches("[a-z]*\\d+[a-z0-9]*")) {
                    return token;
                }
            }
        }

        return importantModel.toString().trim();
    }

    private String normalizeForSimilarity(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        normalized = MODEL_CODE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = STORAGE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NETWORK_PATTERN.matcher(normalized).replaceAll(" ");

        for (Pattern colorPattern : COLOR_PATTERNS) {
            normalized = colorPattern.matcher(normalized).replaceAll(" ");
        }

        normalized = NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private double jaroWinklerSimilarity(String s1, String s2) {
        double jaro = jaroSimilarity(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2);
        return jaro + (prefixLength * 0.1 * (1.0 - jaro));
    }

    private double jaroSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0d;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0 || len2 == 0) {
            return 0.0d;
        }

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matches[j]) {
                    continue;
                }
                if (s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }

                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0d;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        transpositions /= 2.0d;

        return ((matches / (double) len1)
                + (matches / (double) len2)
                + ((matches - transpositions) / matches)) / 3.0d;
    }

    private int commonPrefixLength(String s1, String s2) {
        int length = Math.min(Math.min(s1.length(), s2.length()), MAX_PREFIX_LENGTH);
        int prefix = 0;

        for (int i = 0; i < length; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
            prefix++;
        }

        return prefix;
    }

    private static Pattern wordPattern(String value) {
        String regex = "\\b" + value.replace(" ", "\\s+") + "\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
}



