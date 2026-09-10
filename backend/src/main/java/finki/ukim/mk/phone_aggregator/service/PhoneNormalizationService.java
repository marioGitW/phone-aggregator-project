package finki.ukim.mk.phone_aggregator.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class PhoneNormalizationService {

    private static final Pattern SAMSUNG_MODEL_CODE_PATTERN = Pattern.compile("\\bSM-[A-Z0-9]+\\b", Pattern.CASE_INSENSITIVE);
    // Apple part numbers - "MYE73Z", "MD1Q4", "MYE93HX/A" - are a letter (always "M" in
    // practice) followed by a mixed alphanumeric run that includes at least one digit,
    // optionally with a "/<region letter>" suffix. The digit requirement (the lookahead)
    // is what keeps this from also eating legitimate words like "mini" or "max", which are
    // all letters.
    private static final Pattern APPLE_SKU_PATTERN = Pattern.compile(
            "\\bm(?=[a-z0-9]*\\d)[a-z0-9]{3,8}(?:/[a-z])?\\b", Pattern.CASE_INSENSITIVE);
    // Honor part numbers as scraped - "(5109CEHC)", "(5109CEHA)" - are a bare digit run
    // fused directly to a letter run with no separator, usually parenthesized. Nothing else
    // in a title fuses a unit onto a number without a recognizable suffix like "gb"/"mp"/
    // "mah", so the {3,5}-letter run (longer than any real unit abbreviation) keeps this
    // from clipping storage/camera/battery figures.
    private static final Pattern HONOR_SKU_PATTERN = Pattern.compile(
            "\\b\\d{3,4}[a-z]{3,5}\\b", Pattern.CASE_INSENSITIVE);
    // Macedonian screen-size text - "6,3 <inchi>", "6.77 <inchi>" (Cyrillic "inchi" means
    // inches; spelled below via regex unicode escapes rather than a literal Cyrillic
    // string, to avoid any source-file encoding risk). The word itself disappears under
    // the non-alphanumeric strip below regardless (Cyrillic isn't in [a-z0-9]), but the
    // digits in front of it are alphanumeric and survive on their own, fragmenting
    // modelKey with a stray "6 3" or "6 77". Must run before that strip.
    // No trailing \b: java.util.regex's \b is ASCII-only by default (Cyrillic isn't a "word"
    // character to it), so a boundary assertion placed right after the Cyrillic word can
    // never succeed - there's no \w/\W transition when neither side counts as \w.
    private static final Pattern SCREEN_SIZE_PATTERN = Pattern.compile(
            "\\b\\d+(?:[.,]\\d+)?\\s*\\u0438\\u043d\\u0447\\u0438");
    private static final Pattern STORAGE_RAM_PATTERN = Pattern.compile(
            "(?i)" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*/\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*\\+\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\s*ram\\s*\\d+\\s*(?:gb|tb|mb)?\\b|" +
                    "\\b\\d+\\s*(?:gb|tb|mb)\\b|" +
                    "\\b\\d+\\s*\\/\\s*\\d+\\s*(?:gb|tb|mb)?\\b"
    );
    private static final Pattern NETWORK_PATTERN = Pattern.compile("(?i)\\b(?:5g|4g|lte|dual\\s+sim|sim)\\b");
    // Any "+" surviving past STORAGE_RAM_PATTERN (which already consumes tight ram+storage
    // combos like "12+256gb") is a plus-variant marker - "S26+", "Note 14 Pro+" - and must
    // become a literal "plus" token, not just vanish, or the non-alphanumeric strip below
    // collapses "S26+" and "S26" into the same modelKey.
    // BUT: a looser "128gb + 6gb" (unit stated on the storage side) isn't fully consumed by
    // STORAGE_RAM_PATTERN - each side matches its own "lone unit" alternative separately,
    // leaving the "+" behind - and neither is a "5g + 4g" network-list connector. Both of
    // those leave the "+" surrounded by whitespace (STORAGE_RAM_PATTERN/NETWORK_PATTERN
    // replace their matches with a space, which lands immediately next to the leftover "+"
    // either way, regardless of the original spacing) - a genuine model marker like "s26+"
    // or "pro+" never does, since the letters/digits right before it are never stripped.
    // Requiring no whitespace immediately before the "+" is what tells them apart.
    private static final Pattern PLUS_PATTERN = Pattern.compile("(?<!\\s)\\+");
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
        normalized = APPLE_SKU_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = HONOR_SKU_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = SCREEN_SIZE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = STORAGE_RAM_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NETWORK_PATTERN.matcher(normalized).replaceAll(" ");

        for (Pattern colorPattern : COLOR_PATTERNS) {
            normalized = colorPattern.matcher(normalized).replaceAll(" ");
        }

        // The word "plus" (already spelled out) needs no handling here - only the "+"
        // symbol needs converting so both spellings converge on the same token before
        // the non-alphanumeric strip would otherwise destroy the "+" form.
        normalized = PLUS_PATTERN.matcher(normalized).replaceAll(" plus ");

        normalized = NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ");

        return normalized.replaceAll("\\s+", " ").trim();
    }

    private static Pattern wordPattern(String value) {
        String regex = "\\b" + value.replace(" ", "\\s+") + "\\b";
        return Pattern.compile(regex);
    }
}


