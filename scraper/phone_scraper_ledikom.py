import argparse
import itertools
import json
import logging
import os
import re
import time
from collections import OrderedDict

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, format_title, clean_price, extract_image_url
from utils.spec_extractor import RAM_VALID, STORAGE_VALID

BASE_URL = "https://ledikom.mk"
IMAGE_SELECTOR = "#content > section > div > div > div:nth-child(2) > div > div > a > img"
PRODUCT_CARD_SELECTOR = ".item-in-grid"

# Roughly half of this source's typical full-run yield of unique /p/ product
# pages (measured pre-variant-expansion — this guards the grid selector, not
# the final variant-fanned-out phone count). A full run (no --limit) coming
# in under this means PRODUCT_CARD_SELECTOR likely stopped matching the live
# markup — log loudly instead of silently importing a partial dataset.
MIN_EXPECTED_PRODUCTS = 33

BRAND_URLS = {
    "apple": "https://ledikom.mk/c/416/telefoni/apple-iphone",
    "samsung": "https://ledikom.mk/c/421/telefoni/samsung",
    "xiaomi": "https://ledikom.mk/c/424/telefoni/xiaomi",
    "google": "https://ledikom.mk/c/413/telefoni/google",
    "honor": "https://ledikom.mk/c/411/telefoni/honor",
    "oneplus": "https://ledikom.mk/c/441/telefoni/oneplus",
}

# Bump this whenever the set of fields we scrape/cache per variant changes
# (new field added, extraction logic fixed, etc.) — a stale cache built
# under the old schema must never silently mask the fix. Bumping it both
# routes new runs to a fresh cache file (old file is left untouched, never
# read again) and, as a second line of defense, makes load_variant_cache()
# drop any entry whose stamped version doesn't match.
CACHE_SCHEMA_VERSION = 2

VARIANT_CACHE_FILE = os.path.join(
    os.path.dirname(__file__), f"ledikom_variant_cache.v{CACHE_SCHEMA_VERSION}.json"
)

_GB_NUMBER_RE = re.compile(r"(\d+)\s*(gb|tb)", re.IGNORECASE)

logger = logging.getLogger(__name__)


class Phone:
    def __init__(self, brand, title, rawTitle, siteLink, price, imageUrl=None,
                 ram_gb=None, storage_gb=None, color_raw=None, model_code=None, variant_key=None):
        self.brand = brand
        self.title = title
        self.rawTitle = rawTitle
        self.siteLink = siteLink
        self.price = price
        self.imageUrl = imageUrl
        # ledikom's specs come from the HTML (spec block / variant buttons),
        # never from title parsing — model_code is left None since ledikom
        # doesn't expose Samsung model codes anywhere we scrape.
        self.ram_gb = ram_gb
        self.storage_gb = storage_gb
        self.color_raw = color_raw
        self.model_code = model_code
        # Stable per-variant identity independent of where ledikom's own URL
        # happens to land (it collapses whichever combo it treats as the
        # default SKU onto the bare /p/<id>/ URL, so siteLink alone can't be
        # trusted to be unique across runs). See resolve_variant_state().
        self.variant_key = variant_key

    def to_dict(self):
        return {
            "brand": self.brand,
            "title": self.title,
            "rawTitle": self.rawTitle,
            "siteLink": self.siteLink,
            "price": self.price,
            "imageUrl": self.imageUrl,
            "ram_gb": self.ram_gb,
            "storage_gb": self.storage_gb,
            "color_raw": self.color_raw,
            "model_code": self.model_code,
            "variant_key": self.variant_key,
        }

    @classmethod
    def from_dict(cls, data):
        # Tolerate extra bookkeeping keys (e.g. the cache's
        # "_cache_schema_version" stamp) that aren't Phone fields.
        data = {k: v for k, v in data.items() if not k.startswith("_")}
        return cls(**data)

    def __repr__(self):
        return (f"Phone(brand={self.brand}, title={self.title}, rawTitle={self.rawTitle}, "
                f"siteLink={self.siteLink}, price={self.price}, ram_gb={self.ram_gb}, "
                f"storage_gb={self.storage_gb}, color_raw={self.color_raw}, "
                f"model_code={self.model_code}, "
                f"variant_key={self.variant_key})")



def get_driver():
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    return webdriver.Chrome(options=options)


def wait_for_products(driver, timeout=15):
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, PRODUCT_CARD_SELECTOR))
        )
        time.sleep(2)
        return True
    except:
        return False


def load_variant_cache(use_cache=True):
    if not use_cache:
        return {}
    if not os.path.exists(VARIANT_CACHE_FILE):
        return {}
    try:
        with open(VARIANT_CACHE_FILE, encoding="utf-8") as f:
            raw_cache = json.load(f)
    except Exception as e:
        logger.warning(f"[cache] failed to load {VARIANT_CACHE_FILE}: {e}")
        return {}

    # Defense in depth: the versioned filename already keeps old-schema
    # caches from ever being opened, but also drop any individual entry
    # that isn't stamped with the current schema version (e.g. a file
    # edited/merged by hand) instead of trusting it blindly.
    cache = {}
    dropped = 0
    for key, entry in raw_cache.items():
        if isinstance(entry, dict) and entry.get("_cache_schema_version") == CACHE_SCHEMA_VERSION:
            cache[key] = entry
        else:
            dropped += 1
    if dropped:
        logger.info(f"[cache] dropped {dropped} entr{'y' if dropped == 1 else 'ies'} "
                    f"with mismatched schema version from {VARIANT_CACHE_FILE}")
    return cache


def save_variant_cache(cache, use_cache=True):
    if not use_cache:
        return
    try:
        with open(VARIANT_CACHE_FILE, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        logger.warning(f"[cache] failed to save {VARIANT_CACHE_FILE}: {e}")


def _parse_gb_number(*texts):
    """Pull the first "<number>GB" or "<number>TB" (converted to GB, x1024)
    out of any of the given strings. Needed because some products (e.g. the
    iPhone Pro Max line) mix GB and TB options within the same storage
    variant group ("256 GB", "512 GB", "1 TB", "2 TB") — treating "TB" as
    unrecognized used to make the whole group fail its all-numeric check and
    get silently dropped, losing storage_gb for every variant of that
    product, not just the TB ones."""
    for text in texts:
        if not text:
            continue
        m = _GB_NUMBER_RE.search(text)
        if m:
            value = int(m.group(1))
            if m.group(2).lower() == "tb":
                value *= 1024
            return value
    logger.debug(f"_parse_gb_number: no <number>GB/TB found in {texts!r}")
    return None


_PRODUCT_ID_RE = re.compile(r"/p/(\d+)/")


def extract_product_id(base_url):
    m = _PRODUCT_ID_RE.search(base_url or "")
    return m.group(1) if m else None


def build_variant_key(product_id, combo):
    """
    Deterministic per-variant identity built from the product id plus the
    sorted option ids selected for this combo, e.g. "ledikom:6035:12772,12781".
    Known before navigating, so it doesn't depend on where ledikom's own URL
    lands (see the Phone.variant_key docstring).
    """
    option_ids = sorted(opt["id"] for opt in combo if opt.get("id"))
    suffix = ",".join(option_ids)
    if product_id and suffix:
        return f"ledikom:{product_id}:{suffix}"
    if product_id:
        return f"ledikom:{product_id}"
    return None


def assert_unique_variant_keys(phones, product_url):
    """Self-check: every variant of a product must have a unique variant_key."""
    keys = [p.variant_key for p in phones]
    seen = set()
    duplicates = set()
    for k in keys:
        if k in seen:
            duplicates.add(k)
        seen.add(k)

    if duplicates:
        logger.error(f"DUPLICATE variant_key(s) for {product_url}: {sorted(duplicates)}")
        for k in sorted(duplicates):
            offenders = [p.siteLink for p in phones if p.variant_key == k]
            logger.error(f"  {k} -> {offenders}")
        return False
    return True


def _text_content(driver, el):
    """
    Read an element's text via JS textContent instead of Selenium's .text.
    .text returns "" for elements inside a display:none ancestor — and the
    spec block (div.spec-collapse-body) is collapsed by default until its
    toggle is clicked, so .text silently loses this data even though it's
    present in the DOM.
    """
    try:
        return (driver.execute_script("return arguments[0].textContent;", el) or "").strip()
    except Exception:
        return ""


def read_spec_ram(driver):
    """Read RAM from div.spec-collapse-body ul.item-owner > li > span, span label/value pairs.

    Uses textContent (not .text) because the spec block is collapsed
    (display:none) until its "+" toggle is clicked, and .text returns ""
    for non-displayed elements regardless of what's actually in the DOM.
    """
    try:
        rows = driver.find_elements(
            By.CSS_SELECTOR, "div.spec-collapse-body ul.item-owner > li"
        )
        for row in rows:
            spans = row.find_elements(By.CSS_SELECTOR, "span")
            if len(spans) < 2:
                continue
            label = _text_content(driver, spans[0]).lower()
            value = _text_content(driver, spans[1])
            if "ram" in label or "рам" in label:
                ram = _parse_gb_number(value)
                if ram is not None:
                    return ram
    except Exception as e:
        logger.warning(f"failed reading spec RAM: {e}")
    return None


def read_price(driver):
    try:
        el = driver.find_element(By.CSS_SELECTOR, ".price .new_price")
        return clean_price(el.text)
    except Exception:
        pass
    try:
        el = driver.find_element(By.CSS_SELECTOR, ".price")
        return clean_price(el.text)
    except Exception as e:
        logger.warning(f"failed reading price: {e}")
        return None


def parse_variant_groups(driver):
    """
    Find variant groups: each is the common container of a set of a.variant-btn
    elements (rendered as <div class="row [color-design]"><label>...</label>
    <div>...buttons...</div></div> on ledikom product pages).

    Returns an ordered list of groups, each:
        {"kind": "color"|"storage"|"ram", "options": [option, ...]}
    where option is:
        {"id": str, "href": str|None, "value": str|int, "active": bool}
    Unclassifiable groups are logged and dropped (their axis stays None on
    every emitted Phone).
    """
    groups = OrderedDict()  # container element id -> {"container": el, "buttons": [...]}
    buttons = driver.find_elements(By.CSS_SELECTOR, "a.variant-btn")
    for btn in buttons:
        try:
            container = btn.find_element(By.XPATH, "./../..")
        except Exception:
            continue
        key = container.id
        groups.setdefault(key, {"container": container, "buttons": []})["buttons"].append(btn)

    parsed_groups = []
    for group in groups.values():
        container = group["container"]
        buttons = group["buttons"]

        is_color = any(b.find_elements(By.TAG_NAME, "img") for b in buttons)

        label_text = ""
        try:
            label_els = container.find_elements(By.CSS_SELECTOR, "strong, label")
            if label_els:
                label_text = label_els[0].text.strip().lower()
        except Exception:
            pass

        options = []
        for b in buttons:
            img_alt = ""
            if is_color:
                imgs = b.find_elements(By.TAG_NAME, "img")
                if imgs:
                    img_alt = (imgs[0].get_attribute("alt") or "").strip()

            options.append({
                "id": b.get_attribute("data-variant-property-value-id"),
                "href": b.get_attribute("href"),
                "title_attr": (b.get_attribute("title") or "").strip(),
                "text": b.text.strip(),
                "img_alt": img_alt,
                "active": "active" in (b.get_attribute("class") or ""),
            })

        if is_color:
            for opt in options:
                opt["value"] = opt["title_attr"] or opt["img_alt"] or opt["text"] or None
            parsed_groups.append({"kind": "color", "options": options})
            continue

        numeric_options = []
        all_numeric = True
        for opt in options:
            n = _parse_gb_number(opt["title_attr"], opt["text"])
            opt["value"] = n
            if n is None:
                all_numeric = False
            numeric_options.append(n)

        if not all_numeric:
            logger.warning(f"could not classify variant group (label={label_text!r}); skipping axis")
            continue

        if "рам" in label_text or "ram" in label_text:
            kind = "ram"
        elif "мемориј" in label_text or "memory" in label_text or "storage" in label_text:
            kind = "storage"
        else:
            # Fall back to value-range heuristics shared with spec_extractor.
            if all(n in RAM_VALID for n in numeric_options):
                kind = "ram"
            elif all(n in STORAGE_VALID for n in numeric_options):
                kind = "storage"
            else:
                for n in numeric_options:
                    if n not in RAM_VALID and n not in STORAGE_VALID:
                        logger.debug(f"rejected variant group value {n} "
                                     f"(not in RAM_VALID or STORAGE_VALID)")
                logger.warning(f"ambiguous variant group values={numeric_options}; skipping axis")
                continue

        parsed_groups.append({"kind": kind, "options": options})

    return parsed_groups


def resolve_variant_state(driver, base_url, combo):
    """
    Put the driver into the state described by `combo` (one option per group)
    and return the resulting variant URL.

    Uses a direct href navigation when exactly one option needs changing from
    the page's default state and that option carries a real (non-javascript:)
    href. Otherwise falls back to clicking every non-default option in turn,
    starting from a fresh load of the product page, and reads back the
    resulting /v/<id>/ URL.
    """
    non_default = [opt for opt in combo if not opt["active"]]

    if len(non_default) <= 1:
        candidate = non_default[0] if non_default else None
        if candidate and candidate["href"] and not candidate["href"].startswith("javascript"):
            driver.get(candidate["href"])
            time.sleep(1)
            return driver.current_url

    driver.get(base_url)
    time.sleep(1)
    for opt in combo:
        if opt["active"] or not opt["id"]:
            continue
        try:
            el = driver.find_element(
                By.CSS_SELECTOR, f"a.variant-btn[data-variant-property-value-id='{opt['id']}']"
            )
            driver.execute_script("arguments[0].click();", el)
            time.sleep(1)
        except Exception as e:
            logger.warning(f"failed selecting variant option id={opt['id']}: {e}")

    return driver.current_url


def scrape_product_variants(driver, base_url, brand, raw_title, title, image_url,
                             fallback_price, cache):
    """Visit one ledikom /p/ page and return one Phone per variant combination."""
    logger.debug(f"visiting {base_url}")
    try:
        driver.get(base_url)
        time.sleep(1)
    except Exception as e:
        logger.warning(f"failed loading product page {base_url}: {e}")
        logger.debug("ram source: none (page failed to load)")
        return [Phone(brand=brand, title=title, rawTitle=raw_title, siteLink=base_url,
                       price=fallback_price, imageUrl=image_url,
                       variant_key=build_variant_key(extract_product_id(base_url), []))]

    spec_ram = read_spec_ram(driver)
    product_id = extract_product_id(base_url)

    try:
        groups = parse_variant_groups(driver)
    except Exception as e:
        logger.warning(f"failed parsing variant groups for {base_url}: {e}")
        groups = []

    has_ram_group = any(g["kind"] == "ram" for g in groups)
    if has_ram_group:
        ram_source = "variant group"
    elif spec_ram is not None:
        ram_source = "spec block"
    else:
        ram_source = "none"
    logger.debug(f"ram source: {ram_source} (spec_ram={spec_ram}, has_ram_group={has_ram_group})")

    if not groups:
        price = read_price(driver)
        return [Phone(
            brand=brand, title=title, rawTitle=raw_title, siteLink=base_url,
            price=price if price is not None else fallback_price, imageUrl=image_url,
            ram_gb=spec_ram, variant_key=build_variant_key(product_id, []),
        )]

    kinds = [g["kind"] for g in groups]
    option_lists = [g["options"] for g in groups]

    phones = []
    for combo in itertools.product(*option_lists):
        # Keyed by product + the specific option ids selected, so a warm
        # cache can skip navigation entirely instead of re-clicking through
        # to discover a variant URL we already know.
        combo_key = base_url + "::" + ",".join(opt["id"] or "" for opt in combo)

        if combo_key in cache:
            phones.append(Phone.from_dict(cache[combo_key]))
            continue

        try:
            variant_url = resolve_variant_state(driver, base_url, combo)
        except Exception as e:
            logger.warning(f"failed resolving variant combo for {base_url}: {e}")
            variant_url = base_url

        try:
            price = read_price(driver)
        except Exception as e:
            logger.warning(f"failed reading price for {variant_url}: {e}")
            price = None

        color_raw = None
        storage_gb = None
        ram_gb = spec_ram
        for kind, opt in zip(kinds, combo):
            if kind == "color":
                color_raw = opt["value"]
            elif kind == "storage":
                storage_gb = opt["value"]
            elif kind == "ram":
                ram_gb = opt["value"]

        phone = Phone(
            brand=brand, title=title, rawTitle=raw_title,
            siteLink=(variant_url or base_url).lower(),
            price=price if price is not None else fallback_price,
            imageUrl=image_url, ram_gb=ram_gb, storage_gb=storage_gb, color_raw=color_raw,
            variant_key=build_variant_key(product_id, combo),
        )
        cache[combo_key] = {**phone.to_dict(), "_cache_schema_version": CACHE_SCHEMA_VERSION}
        phones.append(phone)

    assert_unique_variant_keys(phones, base_url)

    return phones


def collect_products_from_grid(driver, brand, url):
    """Pass 1: read raw_title/price/link/image straight off the grid page,
    without navigating anywhere, so nothing goes stale before pass 2."""
    logger.info(f"Scraping {brand} -> {url}")
    driver.get(url)

    loaded = wait_for_products(driver)
    if not loaded:
        logger.warning(f"No products found for {brand}")
        return []

    grid_items = driver.find_elements(By.CSS_SELECTOR, PRODUCT_CARD_SELECTOR)
    logger.info(f"Found {len(grid_items)} {brand} phones")

    products = []
    for p in grid_items:
        try:
            raw_title = remove_voucher(p.find_element(By.CSS_SELECTOR, ".item-name a").text.strip()).lower()
            # Use the category-scoped brand (from BRAND_URLS), not a first-word guess off
            # raw_title - ledikom's own titles for Samsung foldables/Xiaomi sub-lines start
            # with "galaxy"/"redmi" rather than "samsung"/"xiaomi", so guessing from the raw
            # title alone previously mis-tagged those listings with a brand of their own.
            title = format_title(raw_title, brand)

            try:
                price_text = p.find_element(By.CSS_SELECTOR, ".grid-new-price").text
            except:
                price_text = p.find_element(By.CSS_SELECTOR, ".price").text
            price = clean_price(price_text)

            link = p.find_element(By.CSS_SELECTOR, "a[href*='/p/']").get_attribute("href")

            try:
                imageUrl = extract_image_url(p, IMAGE_SELECTOR, BASE_URL)
            except:
                imageUrl = None

            products.append({
                "brand": brand,
                "title": title,
                "raw_title": raw_title,
                "link": (link or "").lower(),
                "price": price,
                "imageUrl": imageUrl,
            })

        except Exception as e:
            logger.warning(f"Skipped product (grid read): {e}")

    return products


def scrape_ledikom(limit=None, use_cache=True):
    driver = get_driver()
    phones = []
    seen_product_urls = set()
    cache = load_variant_cache(use_cache=use_cache)

    all_products = []
    for brand, url in BRAND_URLS.items():
        all_products.extend(collect_products_from_grid(driver, brand, url))

    if limit is None and len(all_products) < MIN_EXPECTED_PRODUCTS:
        logger.error(f"ledikom found only {len(all_products)} products "
                     f"(expected at least {MIN_EXPECTED_PRODUCTS}) using selector "
                     f"{PRODUCT_CARD_SELECTOR!r} — it may no longer match the live markup.")

    processed_count = 0
    for product in all_products:
        if product["link"] in seen_product_urls:
            continue
        seen_product_urls.add(product["link"])

        if limit is not None and processed_count >= limit:
            break
        processed_count += 1

        try:
            variant_phones = scrape_product_variants(
                driver,
                base_url=product["link"],
                brand=product["brand"],
                raw_title=product["raw_title"],
                title=product["title"],
                image_url=product["imageUrl"],
                fallback_price=product["price"],
                cache=cache,
            )
        except Exception as e:
            logger.error(f"failed scraping variants for {product['link']}: {e}")
            logger.debug("ram source: none (product-level failure)")
            variant_phones = [Phone(
                brand=product["brand"], title=product["title"], rawTitle=product["raw_title"],
                siteLink=product["link"], price=product["price"], imageUrl=product["imageUrl"],
                variant_key=build_variant_key(extract_product_id(product["link"]), []),
            )]

        for ph in variant_phones:
            logger.debug(f"+ {ph.title} - {ph.price} (ram={ph.ram_gb}, storage={ph.storage_gb}, "
                         f"color={ph.color_raw}, variant_key={ph.variant_key})")
        phones.extend(variant_phones)

        save_variant_cache(cache, use_cache=use_cache)

    driver.quit()
    return phones


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Ledikom phone scraper")
    parser.add_argument(
        "--limit", type=int, default=None,
        help="Only process this many products (across all brands) — for testing."
    )
    parser.add_argument(
        "--no-cache", action="store_true",
        help="Bypass the variant cache entirely — don't read or write "
             f"{os.path.basename(VARIANT_CACHE_FILE)}. Use this to force a "
             "fully fresh scrape (e.g. after fixing extraction logic, "
             "before the version bump had landed)."
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")

    logger.info("Starting Ledikom scraper...")
    phones = scrape_ledikom(limit=args.limit, use_cache=not args.no_cache)

    logger.info(f"Total phones scraped: {len(phones)}")
    for brand in BRAND_URLS.keys():
        count = len([p for p in phones if p.brand == brand])
        logger.info(f"  {brand}: {count} phones")
    for phone in phones:
        logger.debug(phone)