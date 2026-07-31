package finki.ukim.mk.phone_aggregator.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class PhoneNormalizationService {

    private static final Pattern SAMSUNG_MODEL_CODE_PATTERN = Pattern.compile("\\bSM-[A-Z0-9]+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORAGE_RAM_PATTERN = Pattern.compile(
            "(?i)" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*/\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*\\+\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*ram\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\b|" +
                    "\\b\\d+\\s*\\/\\s*\\d+\\s*(?:gb|tb|mb)?\\b"
    );
    private static final Pattern NETWORK_PATTERN = Pattern.compile("(?i)\\b(?:5g|4g|lte|dual\\s+sim|sim)\\b");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");

    private static final List<Pattern> COLOR_PATTERNS = List.of(
            wordPattern("black"),
            wordPattern("white"),
            wordPattern("blue"),
            wordPattern("green"),
            wordPattern("gray"),
            wordPattern("grey"),
            wordPattern("silver"),
            wordPattern("gold"),
            wordPattern("red"),
            wordPattern("pink"),
            wordPattern("purple"),
            wordPattern("yellow"),
            wordPattern("orange"),
            wordPattern("bronze"),
            wordPattern("graphite"),
            wordPattern("lavender"),
            wordPattern("cream"),
            wordPattern("beige"),
            wordPattern("brown"),
            wordPattern("coral"),
            wordPattern("turquoise"),
            wordPattern("navy"),
            wordPattern("cyan"),
            wordPattern("teal"),
            wordPattern("rose"),
            wordPattern("midnight"),
            wordPattern("starlight"),
            wordPattern("light blue"),
            wordPattern("dark blue"),
            wordPattern("space gray"),
            wordPattern("space grey")
    );

    public String normalizeTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "";
        }

        String normalized = rawTitle.toLowerCase(Locale.ROOT);
        normalized = SAMSUNG_MODEL_CODE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = STORAGE_RAM_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NETWORK_PATTERN.matcher(normalized).replaceAll(" ");

        for (Pattern colorPattern : COLOR_PATTERNS) {
            normalized = colorPattern.matcher(normalized).replaceAll(" ");
        }

        normalized = NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ");

        return normalized.replaceAll("\\s+", " ").trim();
    }

    private static Pattern wordPattern(String value) {
        String regex = "\\b" + value.replace(" ", "\\s+") + "\\b";
        return Pattern.compile(regex);
    }
}


