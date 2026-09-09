"""Extract structured specs (RAM, storage, color, 5G, model code) from a scraped rawTitle."""
import re

RAM_VALID = {1, 2, 3, 4, 6, 8, 12, 16}
STORAGE_VALID = {16, 32, 64, 128, 256, 512, 1024, 2048}

# number [unit]? separator number [unit]?  e.g. "8gb/256gb", "6+128gb", "128gb + 6gb", "8/128"
_PAIR_RE = re.compile(r"(\d+)\s*(gb|tb)?\s*[/+,]\s*(\d+)\s*(gb|tb)?", re.IGNORECASE)

# a single number with an explicit unit, e.g. "128gb", "1tb"
_LONE_RE = re.compile(r"(\d+)\s*(gb|tb)", re.IGNORECASE)

_MODEL_CODE_RE = re.compile(r"\bsm-[a-z0-9]+\b", re.IGNORECASE)
_5G_RE = re.compile(r"\b5g\b", re.IGNORECASE)

_COLOR_WORDS = sorted(
    [
        "midnight black", "ocean blue", "sandy gold", "light blue",
        "space gray", "cool blue", "frost blue", "forest green",
        "црн", "црно", "бел", "бела", "син", "сина", "светло син",
        "сив", "зелен", "жолт", "виолетова", "графит", "природен титаниум",
    ],
    key=len,
    reverse=True,
)


def _to_gb(value, unit):
    value = int(value)
    if unit and unit.lower() == "tb":
        return value * 1024
    return value


def _extract_ram_storage(text):
    for match in _PAIR_RE.finditer(text):
        raw1, unit1, raw2, unit2 = match.groups()
        num1 = _to_gb(raw1, unit1)
        num2 = _to_gb(raw2, unit2)
        ram_gb, storage_gb = min(num1, num2), max(num1, num2)
        if ram_gb in RAM_VALID and storage_gb in STORAGE_VALID and storage_gb >= ram_gb:
            return ram_gb, storage_gb

    for match in _LONE_RE.finditer(text):
        raw, unit = match.groups()
        storage_gb = _to_gb(raw, unit)
        if storage_gb in STORAGE_VALID:
            return None, storage_gb

    return None, None


def _extract_color(text):
    for word in _COLOR_WORDS:
        if word in text:
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
