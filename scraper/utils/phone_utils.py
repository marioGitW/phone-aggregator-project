"""Shared utilities for phone scrapers."""
import re
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
        if len(parts) > 2:
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


def extract_image_url(element, image_selector, base_url):
    if not element or not image_selector:
        return None

    try:
        image_element = element.find_element(By.CSS_SELECTOR, image_selector)
    except Exception:
        try:
            image_element = element.find_element(By.CSS_SELECTOR, "img")
        except Exception:
            return None

    src = (image_element.get_attribute("src") or "").strip()
    data_src = (image_element.get_attribute("data-src") or "").strip()

    candidate = src
    if not candidate or "placeholder" in candidate.lower() or candidate.startswith("data:"):
        candidate = data_src

    if not candidate:
        return None

    if candidate.startswith("//"):
        candidate = "https:" + candidate

    return urljoin(base_url, candidate)


