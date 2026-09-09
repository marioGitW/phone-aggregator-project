"""Tests for spec_extractor, driven by real rawTitle values from phones.json."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from utils.spec_extractor import extract_specs


# --- ananas ---------------------------------------------------------------

def test_ananas_slash_gb_suffix_both():
    r = extract_specs("xiaomi мобилен телефон redmi 15c, двојна sim картичка, 8gb/256gb, тиркизен")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256


def test_ananas_comma_separated_gb_suffix_both():
    r = extract_specs("xiaomi паметен телефон redmi note 14 6gb, 128gb, син")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "син"


def test_ananas_slash_no_unit():
    r = extract_specs("xiaomi мобилен телефон poco m5s 4/128 сив")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "сив"


def test_ananas_slash_no_unit_2_32():
    r = extract_specs("xiaomi мобилен телефон redmi 9c 2/32 midnight сив")
    assert r["ram_gb"] == 2
    assert r["storage_gb"] == 32


def test_ananas_slash_storage_has_gb_suffix():
    r = extract_specs("xiaomi мобилен телефон redmi 10 4/64gb сив")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 64


def test_ananas_plus_storage_has_gb_suffix():
    r = extract_specs("xiaomi мобилен телефон 12 lite 8+128gb, црн")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "црн"


def test_ananas_plus_storage_has_gb_suffix_moonlight():
    # Word-boundary matching means "light blue" no longer falsely matches
    # inside "moonlight blue" (the "light" in "moonlight" has no boundary
    # before it) — falls through to bare "blue" instead, which is a real
    # word in the title rather than a coincidental cross-morpheme match.
    r = extract_specs("xiaomi мобилен телефон redmi 15c 4+128gb moonlight blue")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "blue"


def test_ananas_plus_ocean_blue():
    r = extract_specs("xiaomi redmi a5 4+128gb ocean blue")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "ocean blue"


def test_ananas_reordered_storage_gb_plus_ram_gb():
    r = extract_specs("samsung мобилен телефон galaxy а05с црн 4/64gb")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 64
    assert r["color_raw"] == "црн"


def test_ananas_storage_first_pattern():
    r = extract_specs("samsung мобилен телефон galaxy a34 5g + 4g lte , 128gb + 6gb), црн")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True
    assert r["color_raw"] == "црн"


def test_ananas_no_unit_with_5g_token():
    r = extract_specs("samsung мобилен телефон galaxy a56 8/128 5g црно")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True
    assert r["color_raw"] == "црно"


def test_ananas_lone_storage_iphone():
    r = extract_specs("apple паметен телефон iphone 15 pro 128gb експонат, син")
    assert r["ram_gb"] is None
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "син"


def test_ananas_light_blue_via_cool_prefix():
    r = extract_specs("xiaomi мобилен телефон poco m3 pro 5g 6/128 cool син")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True
    assert r["color_raw"] == "син"


def test_ananas_plus_no_unit_ram_storage():
    r = extract_specs("xiaomi мобилен телефон redmi 15 6+128, midnight црн")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "црн"


# --- anhoch ----------------------------------------------------------------

def test_anhoch_gb_slash_gb():
    r = extract_specs("samsung galaxy z fold 8 5g 12gb/256gb cream")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["has_5g"] is True


def test_anhoch_light_blue():
    r = extract_specs("samsung galaxy a57 5g 8gb/256gb light blue")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "light blue"


def test_anhoch_lone_storage_iphone_no_ram():
    r = extract_specs("apple iphone 16 128gb white")
    assert r["ram_gb"] is None
    assert r["storage_gb"] == 128
    assert r["has_5g"] is False


def test_anhoch_unit_only_on_storage():
    r = extract_specs("honor magic7 lite 5g 8/256gb ds titanium black")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["has_5g"] is True


def test_anhoch_sandy_purple_not_sandy_gold():
    r = extract_specs("xiaomi redmi 15 8gb/256gb sandy purple")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "sandy purple"


def test_anhoch_16gb_1tb():
    r = extract_specs("samsung galaxy s26 ultra 5g 16gb/1tb black")
    assert r["ram_gb"] == 16
    assert r["storage_gb"] == 1024


def test_anhoch_bare_blue_color():
    r = extract_specs("apple iphone 15 128gb blue mtp43rxa")
    assert r["ram_gb"] is None
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "blue"


def test_anhoch_4gb_128gb_light_blue():
    r = extract_specs("samsung galaxy a17 a175 4gb/128gb light blue")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "light blue"


# --- ledikom (mostly bare titles, no specs) --------------------------------

def test_ledikom_no_specs_at_all():
    r = extract_specs("xiaomi poco f7 pro")
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None
    assert r["model_code"] is None


def test_ledikom_no_specs_samsung():
    r = extract_specs("samsung galaxy a37")
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None


def test_ledikom_has_5g_no_storage():
    r = extract_specs("samsung galaxy a26 5g")
    assert r["has_5g"] is True
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None


def test_ledikom_no_5g_no_storage():
    r = extract_specs("apple iphone 17")
    assert r["has_5g"] is False
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None


# --- neptun ------------------------------------------------------------

def test_neptun_plus_unit_only_storage():
    r = extract_specs("honor 600 pro 5g 12+512gb golden white")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512
    assert r["has_5g"] is True


def test_neptun_frost_blue():
    r = extract_specs("xiaomi redmi note 14 pro+ 5g 12+512gb frost blue")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512
    assert r["color_raw"] == "frost blue"


def test_neptun_sm_code_with_parens():
    r = extract_specs("samsung galaxy a57 5g 8+128gb (sm-a576bzvbeuc) violet")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 128
    assert r["model_code"] == "SM-A576BZVBEUC"


def test_neptun_sm_code_techno_violet_unknown_color():
    r = extract_specs("samsung galaxy z fold8 ultra 5g 12+512gb (sm-f976bzvceuc) techno violet")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512
    assert r["model_code"] == "SM-F976BZVCEUC"


def test_neptun_slash_gb_suffix_storage_only():
    r = extract_specs("samsung galaxy a26 5g 8/256gb mint")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256


def test_neptun_ocean_blue_source():
    r = extract_specs("xiaomi redmi note 14 pro 4g 8+256gb ocean blue")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["has_5g"] is False
    assert r["color_raw"] == "ocean blue"


# --- setec ---------------------------------------------------------------

def test_setec_model_code_light_blue():
    r = extract_specs("samsung sm-a576 galaxy a57 light blue 12/512gb")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512
    assert r["model_code"] == "SM-A576"
    assert r["color_raw"] == "light blue"


def test_setec_midnight_black():
    r = extract_specs("honor magic8 lite 5g 8gb/256gb midnight black")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "midnight black"


def test_setec_model_code_awesome_olive_color():
    r = extract_specs("samsung sm-a566 galaxy a56 awesome olive 8/256gb")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["model_code"] == "SM-A566"
    assert r["color_raw"] == "awesome olive"


def test_setec_model_code_6gb_slash_128gb():
    r = extract_specs("samsung sm-a276 galaxy a27 5g 6gb/128gb black")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["model_code"] == "SM-A276"


def test_setec_unitless_pair_with_trailing_text():
    r = extract_specs("xiaomi 14 12/512gb black 5g + smart band+redmi pad")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512


def test_setec_fully_unitless_pair():
    r = extract_specs("samsung galaxy s25fe 5g navy 8/256")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256


def test_setec_iphone_lone_storage_light_gold():
    r = extract_specs("apple iphone air 256gb light gold")
    assert r["ram_gb"] is None
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "light gold"


# --- tehnomarket ------------------------------------------------------

def test_tehnomarket_parens_plus_dark_blue_color():
    r = extract_specs("samsung galaxy a57 5g (8+128gb) dark blue")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True
    assert r["color_raw"] == "dark blue"


def test_tehnomarket_slash_unit_storage_only():
    r = extract_specs("xiaomi redmi note 15 pro 8/256gb blue")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256


def test_tehnomarket_mint_green_matches_bare_green():
    # "green" (5 chars) sorts before "mint" (4 chars) in the length-descending
    # word list, so the longer bare color wins the substring match here.
    r = extract_specs("xiaomi redmi 15c 4/128gb mint green")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "green"


def test_tehnomarket_unitless_pair_ultra():
    r = extract_specs("samsung galaxy s25 ultra 12/512gb titanium white silver")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 512


def test_tehnomarket_lone_storage_sky_blue_color():
    r = extract_specs("apple iphone air 256gb sky blue")
    assert r["ram_gb"] is None
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "sky blue"


def test_tehnomarket_5g_gb_slash_gb():
    r = extract_specs("samsung galaxy s25 5g 12gb/128gb silver shadow")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True


def test_tehnomarket_light_violet_parens():
    r = extract_specs("samsung galaxy a37 5g (6+128gb) light violet")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["has_5g"] is True


# --- validation / rejection rules --------------------------------------

def test_2tb_alone_accepted_as_storage():
    r = extract_specs("apple iphone 17 pro max 2tb cosmic orange")
    assert r["storage_gb"] == 2048
    assert r["ram_gb"] is None


def test_1tb_alone_accepted_as_storage():
    r = extract_specs("apple iphone air 1tb space black")
    assert r["storage_gb"] == 1024
    assert r["ram_gb"] is None


def test_empty_title_returns_all_none():
    r = extract_specs("")
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None
    assert r["color_raw"] is None
    assert r["has_5g"] is False
    assert r["model_code"] is None


def test_none_title_returns_all_none():
    r = extract_specs(None)
    assert r["ram_gb"] is None
    assert r["storage_gb"] is None
    assert r["model_code"] is None


def test_priroden_titanium_color():
    r = extract_specs("apple паметен телефон iphone 15 pro 128gb експонат, природен титаниум")
    assert r["color_raw"] == "природен титаниум"


def test_svetlo_sin_preferred_over_sin_substring():
    r = extract_specs("xiaomi мобилен телефон redmi note 14 pro+ 5g, 8gb/256gb, светло син")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "светло син"


# --- extended color dictionary (anhoch colorRaw gap: 20% -> 100%) ---------
# All titles below are real rawTitle values from anhoch (fetched via git
# history since the live site's title text is what drove this gap — see
# scraper/utils/spec_extractor.py's _COLOR_WORDS extension).

def test_anhoch_bare_black():
    r = extract_specs("samsung galaxy xcover7 rugged 5g 6gb/128gb black")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "black"


def test_anhoch_bare_gray():
    r = extract_specs("samsung galaxy a17 a175 4gb/128gb gray")
    assert r["ram_gb"] == 4
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "gray"


def test_anhoch_cobalt_violet():
    r = extract_specs("samsung galaxy s26 5g 12gb/256gb cobalt violet")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "cobalt violet"


def test_anhoch_glacier_blue():
    r = extract_specs("xiaomi redmi note 15 pro 8gb/256gb glacier blue")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "glacier blue"


def test_anhoch_titan_gray():
    r = extract_specs("xiaomi redmi 15 6gb/128gb titan gray")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "titan gray"


def test_anhoch_reddish_brown():
    r = extract_specs("honor magic8 lite 5g 8gb/512gb ds reddish brown")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 512
    assert r["has_5g"] is True
    assert r["color_raw"] == "reddish brown"


def test_anhoch_awesome_graphite():
    r = extract_specs("samsung galaxy a56 5g 8gb/256gb awesome graphite")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "awesome graphite"


def test_anhoch_silver_shadow():
    r = extract_specs("samsung galaxy z fold 7 5g 12gb/256gb silver shadow")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "silver shadow"


def test_anhoch_jet_black():
    r = extract_specs("samsung galaxy z fold 7 5g 12gb/256gb jet black")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "jet black"


def test_anhoch_icyblue_no_space():
    r = extract_specs("samsung galaxy s25 5g 12gb/256gb icyblue")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "icyblue"


def test_anhoch_blueblack():
    r = extract_specs("samsung galaxy s25 5g 12gb/256gb blueblack")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "blueblack"


def test_anhoch_bare_lavander_misspelling():
    r = extract_specs("apple iphone 17 256gb lavander")
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "lavander"


def test_anhoch_mocha_brown():
    r = extract_specs("xiaomi redmi note 15 pro+ 5g 8gb/256gb mocha brown")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "mocha brown"


def test_anhoch_vital_green():
    r = extract_specs("honor 600 lite 5g 8gb/256gb ds vital green")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "vital green"


def test_anhoch_ultramarine():
    r = extract_specs("apple iphone 16 128gb ultramarine")
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "ultramarine"


def test_anhoch_ocean_cyan():
    r = extract_specs("honor x6c 6gb/128gb dual sim ocean cyan")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "ocean cyan"


def test_color_still_unknown_returns_none():
    # A color word genuinely absent from the dictionary should still yield
    # None rather than a wrong guess — the extractor never invents values.
    r = extract_specs("samsung galaxy a11 burgundy")
    assert r["color_raw"] is None


# --- word-boundary color matching (ananas ramGb/colorRaw gap) -------------
# Real ananas rawTitle values that exposed two bugs: bare "red" matching as
# a substring inside "redmi" (brand name, not a color), and a spurious
# comma-adjacent number pairing eating the real RAM digit. Fixed in
# spec_extractor.py's _extract_color (word boundaries) and
# _extract_ram_storage (per-digit-position pair matching).

def test_redmi_bare_name_has_no_false_color_match():
    r = extract_specs("xiaomi redmi 15")
    assert r["color_raw"] is None


def test_ananas_comma_after_model_number_no_longer_eats_ram_digit():
    r = extract_specs("xiaomi мобилен телефон, redmi 15, 8/256gb, полноќно црн")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "црн"


def test_ananas_violetov_masculine_no_false_red_match():
    # Before the word-boundary fix, this fell through to a false "red"
    # match (substring of "redmi") since "виолетов" wasn't recognized and
    # ram_gb was also lost to the comma bug.
    r = extract_specs("xiaomi мобилен телефон, redmi 15, 6/128gb, виолетов")
    assert r["ram_gb"] == 6
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "виолетов"


def test_ananas_teget_color():
    r = extract_specs("samsung мобилен телефон, galaxy a57 5g, 8gb/128gb, тегет")
    assert r["ram_gb"] == 8
    assert r["storage_gb"] == 128
    assert r["color_raw"] == "тегет"


def test_ananas_srebren_color():
    r = extract_specs("samsung мобилен телефон, galaxy s25 edge, 12gb/256gb, сребрен")
    assert r["ram_gb"] == 12
    assert r["storage_gb"] == 256
    assert r["color_raw"] == "сребрен"
