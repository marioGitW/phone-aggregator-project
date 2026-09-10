package finki.ukim.mk.phone_aggregator.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the "+" / "Plus" convergence fix: a trailing "+" must normalize to the same
 * "plus" token as the spelled-out word, and must never be silently stripped down to
 * nothing - otherwise "S26+" and "S26" collide into the same modelKey.
 */
class PhoneNormalizationServiceTest {

    private final PhoneNormalizationService service = new PhoneNormalizationService();

    @Test
    void trailingPlusSignAndSpelledOutPlusConverge() {
        String fromSymbol = service.normalizeTitle("Samsung Galaxy S26+ 5g 12+256gb black");
        String fromWord = service.normalizeTitle("Samsung Galaxy S26 Plus 5g 12/256gb black");

        assertThat(fromSymbol).isEqualTo("samsung galaxy s26 plus");
        assertThat(fromSymbol).isEqualTo(fromWord);
    }

    @Test
    void plusVariantNeverCollapsesIntoBaseModel() {
        String base = service.normalizeTitle("Samsung Galaxy S26 5g 12gb/256gb black");
        String plusVariant = service.normalizeTitle("Samsung Galaxy S26+ 5g 12gb/256gb black");

        assertThat(base).isEqualTo("samsung galaxy s26");
        assertThat(plusVariant).isNotEqualTo(base);
    }

    @Test
    void plusOnAModelWordConverges() {
        String fromSymbol = service.normalizeTitle("Xiaomi Redmi Note 14 Pro+ 5g 8gb/256gb midnight black");
        String fromWord = service.normalizeTitle("Xiaomi Redmi Note 14 Pro Plus 5g 8/256gb midnight black");

        assertThat(fromSymbol).isEqualTo("xiaomi redmi note 14 pro plus");
        assertThat(fromSymbol).isEqualTo(fromWord);
    }

    /**
     * Regression: a "+" that's leftover connective tissue - not a model marker - must not
     * turn into a spurious "plus" token. STORAGE_RAM_PATTERN only fully consumes tight
     * "12+256gb" combos; a looser "128gb + 6gb" (unit stated on the storage side) matches
     * each side separately as a lone unit and leaves the "+" standing. Naively converting
     * every leftover "+" produced modelKey "samsung galaxy a34 plus plus" for real data.
     */
    @Test
    void leftoverConnectivePlusIsNotTreatedAsAModelMarker() {
        String looseStorageRamFormat = service.normalizeTitle(
                "Samsung Galaxy A34 5g + 4g lte, 128gb + 6gb, black");

        assertThat(looseStorageRamFormat).isEqualTo("samsung galaxy a34");
        assertThat(looseStorageRamFormat).doesNotContain("plus");
    }

    @Test
    void networkListConnectivePlusIsNotTreatedAsAModelMarker() {
        String result = service.normalizeTitle("Samsung Galaxy A34 5G + 4G LTE 128GB black");

        assertThat(result).isEqualTo("samsung galaxy a34");
        assertThat(result).doesNotContain("plus");
    }

    // --- Apple SKU codes: anhoch's per-color part number (e.g. "MYE73Z") fragmented an
    // established iPhone 16 bucket into singletons - titles below are the real ones from
    // the audit (ids 108, 95, 110, 107, 111, and 81's own clean sibling).

    @Test
    void appleSkuCodeConvergesWithItsCleanSibling() {
        String withSku = service.normalizeTitle("apple iphone 16 128gb black mye73z");
        String clean = service.normalizeTitle("apple iphone 16 128gb black");

        assertThat(withSku).isEqualTo("apple iphone 16");
        assertThat(withSku).isEqualTo(clean);
    }

    @Test
    void appleSkuCodeStrippedAcrossColorVariants() {
        String white = service.normalizeTitle("apple iphone 16 128gb white mye93hx/a");
        String teal = service.normalizeTitle("apple iphone 16 128gb teal myed3hx/a");
        String clean = service.normalizeTitle("apple iphone 16 128gb black");

        assertThat(white).isEqualTo(clean);
        assertThat(teal).isEqualTo(clean);
        assertThat(white).doesNotContain("mye93hx").doesNotContain("hx");
        assertThat(teal).doesNotContain("myed3hx");
    }

    @Test
    void appleSkuCodeStripDoesNotMergeDifferentIphones() {
        String iphone15 = service.normalizeTitle("apple iphone 15 128gb blue mtp43rxa");
        String iphone16 = service.normalizeTitle("apple iphone 16 128gb black mye73z");
        String iphone16e = service.normalizeTitle("apple iphone 16e 128gb black md1q4");

        assertThat(iphone15).isEqualTo("apple iphone 15");
        assertThat(iphone16e).isEqualTo("apple iphone 16e");
        assertThat(iphone15).isNotEqualTo(iphone16);
        assertThat(iphone16e).isNotEqualTo(iphone16);
    }

    // --- Honor SKU codes: neptun's parenthesized per-color code (e.g. "(5109CEHC)")
    // fragmented one Honor 600 Lite into three singletons - real titles from the audit.

    @Test
    void honorSkuCodeStrippedFromParenthesizedCode() {
        String result = service.normalizeTitle("honor 600 lite 5g 8+256gb green (5109cehc)");

        assertThat(result).isEqualTo("honor 600 lite");
        assertThat(result).doesNotContain("5109").doesNotContain("cehc");
    }

    @Test
    void honorSkuCodeConvergesAcrossColorVariants() {
        String velvetBlack = service.normalizeTitle("honor 600 lite 5g 8+256gb velvet black (5109ceha)");
        String velvetGrey = service.normalizeTitle("honor 600 lite 5g 8+256gb velvet grey (5109cehb)");

        assertThat(velvetBlack).isEqualTo(velvetGrey);
        assertThat(velvetBlack).doesNotContain("5109");
    }

    // --- Macedonian screen-size text: "6,3 \u0438\u043d\u0447\u0438" / "6,77 \u0438\u043d\u0447\u0438" left stray "6"/"3"/"77"
    // digit tokens in modelKey, splitting a Galaxy S26 offer off its 23-offer sibling and
    // a Redmi Note 15 offer off its bucket - real titles from the audit (ids 39, 10, 8).
    // The Cyrillic fragments in these titles are spelled via unicode escapes rather than
    // literal characters to avoid any source-file encoding risk: MOBILEN_TELEFON = "mobile phone"
    // (filler, stripped regardless of this fix), SIN/CRN = "blue"/"black" (colors, likewise
    // already stripped), INCHI = "inches" (what SCREEN_SIZE_PATTERN targets).

    private static final String MOBILEN_TELEFON =
            "\u043c\u043e\u0431\u0438\u043b\u0435\u043d\u0020\u0442\u0435\u043b\u0435\u0444\u043e\u043d";
    private static final String SIN = "\u0441\u0438\u043d";
    private static final String CRN = "\u0446\u0440\u043d";
    private static final String INCHI = "\u0438\u043d\u0447\u0438";

    @Test
    void screenSizeWithCommaDecimalConvergesGalaxyS26WithItsSibling() {
        String withScreenSize = service.normalizeTitle(
                "samsung " + MOBILEN_TELEFON + ", galaxy s26 5g, 12/512gb, 6,3 " + INCHI + ", " + SIN);
        String clean = service.normalizeTitle("samsung galaxy s26 5g 12gb/512gb black");

        assertThat(withScreenSize).isEqualTo("samsung galaxy s26");
        assertThat(withScreenSize).isEqualTo(clean);
    }

    @Test
    void screenSizeWithTwoDigitDecimalConvergesRedmiNote15WithItsSibling() {
        String sixOverOneTwentyEight = service.normalizeTitle(
                "xiaomi " + MOBILEN_TELEFON + ", redmi note 15, 6/128gb, 6,77 " + INCHI + ", " + CRN);
        String eightOverTwoFiftySix = service.normalizeTitle(
                "xiaomi " + MOBILEN_TELEFON + ", redmi note 15, 8/256 gb, 6,77 " + INCHI + ", " + CRN);
        String clean = service.normalizeTitle("xiaomi redmi note 15 8/256 coral green");

        assertThat(sixOverOneTwentyEight).isEqualTo("xiaomi redmi note 15");
        assertThat(eightOverTwoFiftySix).isEqualTo(clean);
    }
}
