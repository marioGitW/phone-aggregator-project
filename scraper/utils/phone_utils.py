"""Shared utilities for phone scrapers."""
import re
import time
from urllib.parse import urljoin

from selenium.webdriver.common.by import By

VOUCHER_PREFIXES = ["ваучер за преднарачка", "ваучер преднарачка"]


def remove_voucher(text):
    if not text:
        return text
    text_lower = text.lower()
    for prefix in VOUCHER_PREFIXES:
        if text_lower.startswith(prefix):
            text = text[len(prefix):].strip()
            text_lower = text.lower()
    return text


def get_brand_from_raw(raw_title):
    if not raw_title:
        return None
    first_word = raw_title.lower().split()[0] if raw_title.split() else None
    return first_word


def format_title(raw_title, brand):

    if not raw_title:
        return ""

    tokens = raw_title.lower().strip().split()
    
    def is_storage_token(token):
        return "gb" in token or "tb" in token

    if brand == "samsung":

        keep = []
        for i, token in enumerate(tokens[:4]):
            if not is_storage_token(token):
                keep.append(token)
        return " ".join(keep).strip()

    return " ".join([t for t in tokens if not is_storage_token(t)]).strip()


def normalize_price(price_text):

    return clean_price(price_text)


def clean_price(raw_price_text):
    """
    Strip currency symbols, whitespace, and thousands separators from a scraped price string,
    and return a plain integer (no decimals, since phone prices in this market don't need cents).
    Must correctly handle both "120,700.00" and "89.990,00" style formatting depending on the site.
    """

    raw_text = "" if raw_price_text is None else str(raw_price_text).strip()
    if not raw_text:
        print("[clean_price] raw='' -> 0")
        return 0

    candidate = re.sub(r"[^\d,.]", "", raw_text.replace("\xa0", " "))
    if not candidate:
        print(f"[clean_price] raw={raw_text!r} -> 0")
        return 0

    # Strip trailing separators with nothing after them - leftover punctuation from
    # currency text like "ден." (e.g. "15.980,00 ден." -> "15.980,00." after stripping
    # letters, leaving a stray trailing dot that breaks decimal/thousands detection)
    candidate = candidate.rstrip(",.")
    if not candidate:
        print(f"[clean_price] raw={raw_text!r} -> 0")
        return 0


    comma_count = candidate.count(",")
    dot_count = candidate.count(".")

    digits = ""
    if comma_count and dot_count:
        decimal_sep = "," if candidate.rfind(",") > candidate.rfind(".") else "."
        integer_part, _, _decimal_part = candidate.rpartition(decimal_sep)
        thousands_sep = "." if decimal_sep == "," else ","
        digits = re.sub(r"\D", "", integer_part.replace(thousands_sep, ""))
    elif comma_count or dot_count:
        sep = "," if comma_count else "."
        parts = candidate.split(sep)

        # if len(parts) > 2:
        #     digits = "".join(parts)

        if len(parts) > 2:
            if len(parts[-1]) == 2:
                # last chunk is a decimal remainder (e.g. "39.980.00" -> cents), drop it
                digits = "".join(parts[:-1])
            else:
                digits = "".join(parts)

        else:
            left, right = parts
            left = left or "0"
            if len(right) == 3 and len(left) <= 3:
                digits = left + right
            elif len(right) <= 2:
                digits = left
            else:
                digits = left + right
    else:
        digits = candidate

    try:
        value = int(digits) if digits else 0
    except ValueError:
        value = 0

    print(f"[clean_price] raw={raw_text!r} -> {value}")
    return value


IMAGE_ATTRIBUTE_NAMES = (
    "currentSrc",
    "src",
    "data-src",
    "data-lazy-src",
    "data-original",
    "data-zoom-image",
    "data-image",
    "data-url",
    "data-srcset",
    "srcset",
)


def _read_element_value(element, attribute_name):
    if not element:
        return ""

    for accessor_name in ("get_attribute", "get_property"):
        try:
            accessor = getattr(element, accessor_name)
        except Exception:
            continue

        try:
            value = accessor(attribute_name)
        except Exception:
            continue

        if value:
            return str(value).strip()

    return ""


def _normalize_image_candidate(candidate):
    if not candidate:
        return None

    candidate = str(candidate).strip()
    if not candidate:
        return None

    lower_candidate = candidate.lower()
    if lower_candidate.startswith(("data:", "blob:")):
        return None
    if "placeholder" in lower_candidate or "svg+xml" in lower_candidate:
        return None

    if candidate.startswith("//"):
        candidate = "https:" + candidate

    return candidate


def _extract_from_srcset(srcset_value):
    srcset_value = (srcset_value or "").strip()
    if not srcset_value:
        return None

    candidates = []
    for part in srcset_value.split(","):
        part = part.strip()
        if not part:
            continue
        url = part.split()[0].strip()
        url = _normalize_image_candidate(url)
        if url:
            candidates.append(url)

    return candidates[-1] if candidates else None


def _extract_from_style(style_value):
    style_value = (style_value or "").strip()
    if not style_value:
        return None

    match = re.search(r"url\(['\"]?(.*?)['\"]?\)", style_value, flags=re.IGNORECASE)
    if not match:
        return None

    return _normalize_image_candidate(match.group(1))


def _collect_image_candidates(image_element):
    candidates = []

    for attribute_name in IMAGE_ATTRIBUTE_NAMES:
        value = _read_element_value(image_element, attribute_name)
        if not value:
            continue

        if attribute_name in {"srcset", "data-srcset"}:
            candidate = _extract_from_srcset(value)
        else:
            candidate = _normalize_image_candidate(value)

        if candidate:
            candidates.append(candidate)

    style_candidate = _extract_from_style(_read_element_value(image_element, "style"))
    if style_candidate:
        candidates.append(style_candidate)

    return candidates


def _resolve_image_url_from_element(image_element):
    if not image_element:
        return None

    candidates = _collect_image_candidates(image_element)
    if candidates:
        return candidates[-1]

    for nested_selector in ("img", "picture source", "source", "[style*='url(']"):
        try:
            nested_element = image_element.find_element(By.CSS_SELECTOR, nested_selector)
        except Exception:
            continue

        candidates = _collect_image_candidates(nested_element)
        if candidates:
            return candidates[-1]

    return None


def _try_scroll_into_view(image_element):
    if not image_element:
        return

    try:
        image_element.location_once_scrolled_into_view
        time.sleep(0.2)
    except Exception:
        pass


def extract_image_url(element, image_selector, base_url):
    if not element or not image_selector:
        return None

    selectors_to_try = [image_selector]
    if image_selector != "img":
        selectors_to_try.extend(["img", "picture source", "source"])

    for selector in selectors_to_try:
        try:
            image_element = element.find_element(By.CSS_SELECTOR, selector)
        except Exception:
            continue

        candidate = _resolve_image_url_from_element(image_element)
        if candidate:
            return urljoin(base_url, candidate)

        _try_scroll_into_view(image_element)
        candidate = _resolve_image_url_from_element(image_element)
        if candidate:
            return urljoin(base_url, candidate)

    return None


