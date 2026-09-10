package finki.ukim.mk.phone_aggregator.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers marketing-name-to-base-color collapsing, and a regression: java.util.regex's
 * \b is ASCII-only by default, so a boundary at either edge of a purely-Cyrillic colorRaw
 * (the common case - spec_extractor.py hands back just the color word, nothing else) saw
 * a \W-to-\W "transition" and never matched, silently canonicalizing every Macedonian
 * color to null.
 */
class ColorCanonicalizationServiceTest {

    private final ColorCanonicalizationService service = new ColorCanonicalizationService();

    @Test
    void bareCyrillicColorsCanonicalize() {
        assertThat(service.canonicalize("\u0442\u0438\u0440\u043a\u0438\u0437\u0435\u043d")).isEqualTo("turquoise");
        assertThat(service.canonicalize("\u0437\u043b\u0430\u0442\u043d\u043e\u002d\u043f\u0435\u0441\u043e\u0447\u0435\u043d")).isEqualTo("gold");
        assertThat(service.canonicalize("\u0446\u0440\u043d")).isEqualTo("black");
        assertThat(service.canonicalize("\u0441\u0438\u0432")).isEqualTo("gray");
    }

    @Test
    void marketingNamesCollapseToBaseColor() {
        assertThat(service.canonicalize("cobalt violet")).isEqualTo("violet");
        assertThat(service.canonicalize("titan gray")).isEqualTo("gray");
        assertThat(service.canonicalize("Titanium Gray")).isEqualTo("gray");
        assertThat(service.canonicalize("glacier blue")).isEqualTo("blue");
    }

    @Test
    void awesomeLinePrefixCollapsesToBaseColor() {
        assertThat(service.canonicalize("awesome black")).isEqualTo("black");
        assertThat(service.canonicalize("awesome lime")).isEqualTo("lime");
        assertThat(service.canonicalize("awesome graphite")).isEqualTo("graphite");
        assertThat(service.canonicalize("awesome lavander")).isEqualTo("lavender");
    }

    @Test
    void bareEnglishColorIsAlreadyBase() {
        assertThat(service.canonicalize("Black")).isEqualTo("black");
        assertThat(service.canonicalize("grey")).isEqualTo("gray");
    }

    @Test
    void blankOrUnrecognizedReturnsNull() {
        assertThat(service.canonicalize(null)).isNull();
        assertThat(service.canonicalize("  ")).isNull();
        assertThat(service.canonicalize("holographic shimmer")).isNull();
    }
}
