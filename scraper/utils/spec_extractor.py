"""Extract structured specs (RAM, storage, color, 5G, model code) from a scraped rawTitle."""
import logging
import re

logger = logging.getLogger(__name__)

RAM_VALID = {1, 2, 3, 4, 6, 8, 12, 16}
STORAGE_VALID = {16, 32, 64, 128, 256, 512, 1024, 2048}

# number [unit]? separator number [unit]?  e.g. "8gb/256gb", "6+128gb", "128gb + 6gb",
# "8/128", "8 gb и 256 gb" (Macedonian "и" = "and", seen on ananas).
_PAIR_RE = re.compile(r"(\d+)\s*(gb|tb)?\s*(?:[/+,]|\bи\b)\s*(\d+)\s*(gb|tb)?", re.IGNORECASE)

# a single number with an explicit unit, e.g. "128gb", "1tb"
_LONE_RE = re.compile(r"(\d+)\s*(gb|tb)", re.IGNORECASE)

_DIGIT_RUN_RE = re.compile(r"\d+")

_MODEL_CODE_RE = re.compile(r"\bsm-[a-z0-9]+\b", re.IGNORECASE)
_5G_RE = re.compile(r"\b5g\b", re.IGNORECASE)

_COLOR_WORDS_RAW = sorted(
    [
        "midnight black", "ocean blue", "sandy gold", "light blue",
        "space gray", "cool blue", "frost blue", "forest green",
        "црн", "црно", "бел", "бела", "син", "сина", "светло син",
        "сив", "зелен", "жолт", "виолетова", "виолетов", "графит",
        "природен титаниум", "тегет", "сребрен",
        "златно-песочен", "златен", "тиркизен",

        # Compound / marketing color names actually seen in scraped titles
        # (Samsung "awesome X" line, Apple, Xiaomi, Honor), longest-first so
        # e.g. "sky blue" wins over bare "blue".
        "sky blue", "glacier blue", "cobalt violet", "light violet",
        "deep blue", "dark blue", "dark green", "light pink", "light green",
        "light gold", "mocha brown", "reddish brown", "titan gray",
        "titanium gray", "titanium black", "titanium purple",
        "titanium white silver", "sandy purple", "awesome lime",
        "awesome lavander", "awesome lavender", "awesome white", "awesome black",
        "awesome pink", "awesome graphite", "awesome olive", "awesome lightgray",
        "violet shadow", "blue shadow", "silver shadow", "jet black",
        "icy blue", "icyblue", "lavander purple", "lavender purple",
        "vital green", "velvet gray", "velvet black", "blueblack",
        "mist purple", "mist blue", "space black", "aurora purple",
        "cosmic orange", "coral red", "cloud white", "deep violet",
        "golden white", "ocean cyan",

        # Bare single-word colors — kept last/shortest so any compound above
        # always matches first via the length-descending sort.
        "black", "white", "gray", "grey", "blue", "silver", "green", "pink",
        "purple", "violet", "orange", "cream", "graphite", "mint", "navy",
        "teal", "sage", "gold", "red", "coral", "cyan", "titanium",
        "ultramarine", "lavander", "lavender", "jetblack",
    ],
    key=len,
    reverse=True,
)

# Word-boundary matched (not plain substring) so a short bare color like
# "red" can't false-positive match inside an unrelated word — e.g. "red" is
# a substring of the Xiaomi "Redmi" brand name, which isn't a color at all.
_COLOR_WORDS = [
    (word, re.compile(r"\b" + re.escape(word) + r"\b", re.IGNORECASE))
    for word in _COLOR_WORDS_RAW
]


def _to_gb(value, unit):
    value = int(value)
    if unit and unit.lower() == "tb":
        return value * 1024
    return value


def _extract_ram_storage(text):
    # Try a pair match anchored at *every* digit run's start position, not
    # just re.finditer's non-overlapping matches. A model number followed by
    # a comma (e.g. "redmi 15, 8/256gb") forms a spurious "(15, 8)" pair
    # candidate that finditer tries first; it correctly gets rejected
    # (15 isn't a valid storage size) but finditer then resumes scanning
    # *after* that consumed "8", so it never retries "8/256gb" as a fresh
    # pair starting at "8" — silently losing a real, valid pair later in the
    # string. Anchoring at every digit position instead means each digit
    # always gets its own attempt regardless of what an earlier, invalid
    # candidate consumed.
    for digit_run in _DIGIT_RUN_RE.finditer(text):
        match = _PAIR_RE.match(text, digit_run.start())
        if not match:
            continue
        raw1, unit1, raw2, unit2 = match.groups()
        num1 = _to_gb(raw1, unit1)
        num2 = _to_gb(raw2, unit2)
        ram_gb, storage_gb = min(num1, num2), max(num1, num2)
        if ram_gb in RAM_VALID and storage_gb in STORAGE_VALID and storage_gb >= ram_gb:
            return ram_gb, storage_gb
        logger.debug(f"rejected pair candidate {match.group(0)!r}: "
                     f"ram_gb={ram_gb} (valid={ram_gb in RAM_VALID}), "
                     f"storage_gb={storage_gb} (valid={storage_gb in STORAGE_VALID})")

    for match in _LONE_RE.finditer(text):
        raw, unit = match.groups()
        storage_gb = _to_gb(raw, unit)
        if storage_gb in STORAGE_VALID:
            return None, storage_gb
        logger.debug(f"rejected lone storage candidate {match.group(0)!r}: "
                     f"storage_gb={storage_gb} not in STORAGE_VALID")

    return None, None


def _extract_color(text):
    for word, pattern in _COLOR_WORDS:
        if pattern.search(text):
            return word
    return None


def _extract_model_code(raw_title):
    match = _MODEL_CODE_RE.search(raw_title)
    return match.group(0).upper() if match else None


def extract_specs(raw_title):
    if not raw_title:
        return {
            "ram_gb": None,
            "storage_gb": None,
            "color_raw": None,
            "has_5g": False,
            "model_code": None,
        }

    text = raw_title.lower()
    ram_gb, storage_gb = _extract_ram_storage(text)

    return {
        "ram_gb": ram_gb,
        "storage_gb": storage_gb,
        "color_raw": _extract_color(text),
        "has_5g": bool(_5G_RE.search(text)),
        "model_code": _extract_model_code(raw_title),
    }
