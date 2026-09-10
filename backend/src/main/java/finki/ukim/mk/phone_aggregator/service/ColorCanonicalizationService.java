package finki.ukim.mk.phone_aggregator.service;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Maps the free-text colorRaw scraped from a listing (Macedonian or English, marketing
 * names included) down to one of a small, fixed set of base color names - "cobalt violet"
 * and "titan gray" and "glacier blue" all collapse to the underlying color ("violet",
 * "gray", "blue"), the same way every "awesome X" Samsung marketing name collapses to X.
 * colorRaw keeps the site's original wording untouched; colorCanonical (this service's
 * output) is what grouping/deduplication - e.g. the product page's "available colors" -
 * is done on, since two sites calling the same phone "Cobalt Violet" and "Titan Violet"
 * need to land in the same bucket.
 * <p>
 * Longest-phrase-first so a compound name like "space gray" wins over the bare "gray"
 * that's also a substring of it. Returns null rather than guessing when colorRaw is
 * blank or matches nothing in the map.
 */
@Service
public class ColorCanonicalizationService {

    private static final Map<String, String> CANONICAL_BY_RAW = buildMap();

    private record ColorMatcher(Pattern pattern, String canonical) {
    }

    // UNICODE_CHARACTER_CLASS is required for \b to work at all on the Cyrillic entries:
    // java.util.regex's \b is ASCII-only by default (a Cyrillic letter counts as \W, not
    // \w), so a boundary placed at either edge of a purely-Cyrillic word like "тиркизен"
    // sees a \W-to-\W "transition" and never matches - silently dropping every Macedonian
    // color to null. This flag makes \b (and CASE_INSENSITIVE) Unicode-aware instead.
    private static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final List<ColorMatcher> MATCHERS_LONGEST_FIRST = CANONICAL_BY_RAW.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
            .map(e -> new ColorMatcher(
                    Pattern.compile("\\b" + Pattern.quote(e.getKey()) + "\\b", PATTERN_FLAGS),
                    e.getValue()))
            .toList();

    public String canonicalize(String colorRaw) {
        if (colorRaw == null || colorRaw.isBlank()) {
            return null;
        }

        String text = colorRaw.toLowerCase(Locale.ROOT).trim();

        for (ColorMatcher matcher : MATCHERS_LONGEST_FIRST) {
            if (matcher.pattern().matcher(text).find()) {
                return matcher.canonical();
            }
        }

        return null;
    }

    private static Map<String, String> buildMap() {
        Map<String, String> map = new LinkedHashMap<>();

        // Macedonian -> base English color
        map.put("светло син", "blue");
        map.put("природен титаниум", "titanium");
        map.put("златно-песочен", "gold");
        map.put("црно", "black");
        map.put("црн", "black");
        map.put("бела", "white");
        map.put("бел", "white");
        map.put("сина", "blue");
        map.put("син", "blue");
        map.put("сив", "gray");
        map.put("зелен", "green");
        map.put("жолт", "yellow");
        map.put("виолетова", "violet");
        map.put("виолетов", "violet");
        map.put("графит", "graphite");
        map.put("тегет", "navy");
        map.put("сребрен", "silver");
        map.put("златен", "gold");
        map.put("тиркизен", "turquoise");

        // English compound/marketing names -> the base color they're built from. Most are
        // "<qualifier> <color>" (last word wins: "titan gray" -> gray, "cobalt violet" ->
        // violet); "*_shadow" runs the other way ("violet shadow" -> violet, "shadow"
        // isn't a color); "awesome X" (Samsung's line) always resolves to X.
        map.put("midnight black", "black");
        map.put("ocean blue", "blue");
        map.put("sandy gold", "gold");
        map.put("light blue", "blue");
        map.put("space gray", "gray");
        map.put("cool blue", "blue");
        map.put("frost blue", "blue");
        map.put("forest green", "green");
        map.put("sky blue", "blue");
        map.put("glacier blue", "blue");
        map.put("cobalt violet", "violet");
        map.put("light violet", "violet");
        map.put("deep blue", "blue");
        map.put("dark blue", "blue");
        map.put("dark green", "green");
        map.put("light pink", "pink");
        map.put("light green", "green");
        map.put("light gold", "gold");
        map.put("mocha brown", "brown");
        map.put("reddish brown", "brown");
        map.put("titan gray", "gray");
        map.put("titanium gray", "gray");
        map.put("titanium black", "black");
        map.put("titanium purple", "purple");
        map.put("titanium white silver", "silver");
        map.put("sandy purple", "purple");
        map.put("awesome lime", "lime");
        map.put("awesome white", "white");
        map.put("awesome black", "black");
        map.put("awesome pink", "pink");
        map.put("awesome graphite", "graphite");
        map.put("awesome olive", "olive");
        map.put("awesome lightgray", "gray");
        map.put("awesome lavander", "lavender");
        map.put("awesome lavender", "lavender");
        map.put("violet shadow", "violet");
        map.put("blue shadow", "blue");
        map.put("silver shadow", "silver");
        map.put("jet black", "black");
        map.put("lavender purple", "purple");
        map.put("lavander purple", "purple");
        map.put("vital green", "green");
        map.put("velvet gray", "gray");
        map.put("velvet black", "black");
        map.put("mist purple", "purple");
        map.put("mist blue", "blue");
        map.put("space black", "black");
        map.put("aurora purple", "purple");
        map.put("cosmic orange", "orange");
        map.put("coral red", "red");
        map.put("cloud white", "white");
        map.put("deep violet", "violet");
        map.put("golden white", "white");
        map.put("ocean cyan", "cyan");
        map.put("icy blue", "blue");
        map.put("icyblue", "blue");
        map.put("blueblack", "black");
        map.put("jetblack", "black");

        // Bare single-word colors - already base, kept last/shortest so any compound above
        // always matches first via the longest-first search order.
        map.put("black", "black");
        map.put("white", "white");
        map.put("gray", "gray");
        map.put("grey", "gray");
        map.put("blue", "blue");
        map.put("silver", "silver");
        map.put("green", "green");
        map.put("pink", "pink");
        map.put("purple", "purple");
        map.put("violet", "violet");
        map.put("orange", "orange");
        map.put("cream", "cream");
        map.put("graphite", "graphite");
        map.put("mint", "mint");
        map.put("navy", "navy");
        map.put("teal", "teal");
        map.put("sage", "sage");
        map.put("gold", "gold");
        map.put("red", "red");
        map.put("coral", "coral");
        map.put("cyan", "cyan");
        map.put("titanium", "titanium");
        map.put("ultramarine", "ultramarine");
        map.put("lavander", "lavender");
        map.put("lavender", "lavender");
        map.put("yellow", "yellow");
        map.put("bronze", "bronze");
        map.put("olive", "olive");
        map.put("peach", "peach");

        // Sandy/neutral marketing names not built from an obvious base-color word.
        map.put("sand storm", "beige");
        map.put("desert", "beige");
        map.put("fog", "gray");

        return map;
    }
}
