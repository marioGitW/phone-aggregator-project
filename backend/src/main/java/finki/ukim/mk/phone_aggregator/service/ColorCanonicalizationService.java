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
 * names included) to a canonical, English, lowercase color token. Mirrors the color
 * vocabulary the scraper's spec_extractor.py already recognizes in raw titles.
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

    private static final List<ColorMatcher> MATCHERS_LONGEST_FIRST = CANONICAL_BY_RAW.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
            .map(e -> new ColorMatcher(
                    Pattern.compile("\\b" + Pattern.quote(e.getKey()) + "\\b", Pattern.CASE_INSENSITIVE),
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

        // Macedonian -> canonical English
        map.put("светло син", "light blue");
        map.put("природен титаниум", "titanium");
        map.put("златно-песочен", "sandy gold");
        map.put("црно", "black");
        map.put("црн", "black");
        map.put("бела", "white");
        map.put("бел", "white");
        map.put("сина", "blue");
        map.put("син", "blue");
        map.put("сив", "gray");
        map.put("зелен", "green");
        map.put("жолт", "yellow");
        map.put("виолетова", "purple");
        map.put("виолетов", "purple");
        map.put("графит", "graphite");
        map.put("тегет", "navy");
        map.put("сребрен", "silver");
        map.put("златен", "gold");
        map.put("тиркизен", "turquoise");

        // English compound/marketing names -> themselves (already canonical)
        for (String name : new String[]{
                "midnight black", "ocean blue", "sandy gold", "light blue",
                "space gray", "cool blue", "frost blue", "forest green",
                "sky blue", "glacier blue", "cobalt violet", "light violet",
                "deep blue", "dark blue", "dark green", "light pink", "light green",
                "light gold", "mocha brown", "reddish brown", "titan gray",
                "titanium gray", "titanium black", "titanium purple",
                "titanium white silver", "sandy purple", "awesome lime",
                "awesome white", "awesome black", "awesome pink", "awesome graphite",
                "awesome olive", "awesome lightgray", "violet shadow", "blue shadow",
                "silver shadow", "jet black", "lavender purple", "vital green",
                "velvet gray", "velvet black", "mist purple", "mist blue",
                "space black", "aurora purple", "cosmic orange", "coral red",
                "cloud white", "deep violet", "golden white", "ocean cyan",
        }) {
            map.putIfAbsent(name, name);
        }

        // Spelling variants -> canonical English
        map.put("awesome lavander", "awesome lavender");
        map.put("awesome lavender", "awesome lavender");
        map.put("icy blue", "icy blue");
        map.put("icyblue", "icy blue");
        map.put("lavander purple", "lavender purple");
        map.put("blueblack", "blue black");

        // Bare single-word colors -> canonical English (kept last/shortest so any
        // compound above always matches first via the longest-first search order)
        map.put("black", "black");
        map.put("white", "white");
        map.put("gray", "gray");
        map.put("grey", "gray");
        map.put("blue", "blue");
        map.put("silver", "silver");
        map.put("green", "green");
        map.put("pink", "pink");
        map.put("purple", "purple");
        map.put("violet", "purple");
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
        map.put("jetblack", "jet black");

        return map;
    }
}
