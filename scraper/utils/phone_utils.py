"""Shared utilities for phone scrapers."""
import re

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

    if not price_text or price_text == "N/A":
        return "N/A"

    digits_only = re.sub(r"\D", "", str(price_text))
    return digits_only if digits_only else "N/A"


