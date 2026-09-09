import logging
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, clean_price, extract_image_url
from utils.spec_extractor import extract_specs

BASE_URL = "https://ananas.mk"
CATEGORY_URL = f"{BASE_URL}/kategorii/telefoni-foto/mobilni-telefoni/pametni-telefoni"
IMAGE_SELECTOR = "#__next > div.sc-1f6z3vw-0.CSEYd > div.sc-1k1vhoz-0.huxjgP > div > div.sc-1iekrn-3.fVCVGx > div.ais-Hits > div > div:nth-child(2) > div > a > div.sc-v9wo15-5.kNjFky > span > img"
PRODUCT_CARD_SELECTOR = "a[href*='/proizvod/']"

BRANDS = ["samsung", "apple", "xiaomi", "honor"]

logger = logging.getLogger(__name__)

# Roughly half of this source's typical full-run yield. A full run (no
# --limit) coming in under this means PRODUCT_CARD_SELECTOR likely stopped
# matching the live markup — log loudly instead of silently importing a
# partial dataset.
MIN_EXPECTED_PRODUCTS = 27

# words to remove from phone name
NAME_PREFIXES = [
    "мобилен телефон",
    "паметен телефон",
    "mobile phone",
    "smartphone","виолетов","син","црн","sandy","purple","ментол",
    "зелен","полноќно","црно","cool","moonlight","blue","сив","6/128","8/128"
    "black","тиркизна","боја","златно-песок","-","сина","frost","виолетов","8/256",
    "midnight","ocean","жолт"
]




class Phone:
    def __init__(self, brand, title, rawTitle, siteLink, price, imageUrl=None,
                 ram_gb=None, storage_gb=None, color_raw=None, model_code=None):
        self.brand = brand
        self.title = title
        self.rawTitle = rawTitle
        self.siteLink = siteLink
        self.price = price
        self.imageUrl = imageUrl
        self.ram_gb = ram_gb
        self.storage_gb = storage_gb
        self.color_raw = color_raw
        self.model_code = model_code

    def __repr__(self):
        return (f"Phone(brand={self.brand}, title={self.title}, rawTitle={self.rawTitle}, "
                f"siteLink={self.siteLink}, price={self.price}, ram_gb={self.ram_gb}, "
                f"storage_gb={self.storage_gb}, color_raw={self.color_raw}, "
                f"model_code={self.model_code})")


def get_driver():
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument(
        "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
    driver = webdriver.Chrome(options=options)
    return driver


def wait_for_cards(driver, timeout=15):
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "a[href*='/proizvod/']"))
        )
        time.sleep(2)
        return True
    except:
        return False


def detect_brand_from_raw(raw_title):
    # Kept for backwards compatibility, but use get_brand_from_raw instead
    return get_brand_from_raw(raw_title)


def clean_name(name):
    name = name.lower().strip()
    for prefix in NAME_PREFIXES:
        name = name.replace(prefix, "").strip()
    return name


def scrape_all_phones(limit=None):
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        url = f"{CATEGORY_URL}?page={page}"
        logger.info(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            logger.info(f"No cards on page {page}, stopping.")
            break

        # scroll to bottom to trigger lazy loading of all cards
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        time.sleep(2)

        cards = driver.find_elements(By.CSS_SELECTOR, PRODUCT_CARD_SELECTOR)
        logger.info(f"Found {len(cards)} products on page {page}")

        if not cards:
            break

        # duplicate page check
        page_urls = [c.get_attribute("href") for c in cards]
        new_urls = [u for u in page_urls if u not in seen_urls]
        if not new_urls:
            logger.info(f"Page {page} is a duplicate, stopping.")
            break

        for card in cards:
            try:
                href = card.get_attribute("href") or "N/A"
                if href in seen_urls:
                    continue
                seen_urls.add(href)

                if href.startswith("/"):
                    href = BASE_URL + href

                # raw name for brand detection before cleaning
                try:
                    raw_name = card.find_element(By.CSS_SELECTOR, "h3").text.strip()
                except:
                    raw_name = "N/A"

                # Remove voucher and lowercase -> rawTitle
                raw_title = remove_voucher(raw_name).lower()

                # detect brand from raw title (first word)
                brand = get_brand_from_raw(raw_title)
                if brand is None or brand not in BRANDS:
                    continue

                # clean and apply brand-specific formatting -> title
                title = format_title(clean_name(raw_title), brand)

                # price - normalize to only digits
                try:
                    raw_price = ""
                    price = 0
                    price_spans = card.find_elements(By.CSS_SELECTOR, "span")
                    for span in reversed(price_spans):
                        raw_price = span.text.strip()
                        clean = raw_price.replace(".", "").replace(",", "")
                        if clean.isdigit() and len(clean) >= 3:
                            break
                    logger.debug(f"[price] raw={raw_price!r}")
                    price = clean_price(raw_price)
                    logger.debug(f"[price] cleaned={price}")
                except:
                    logger.debug("[price] raw=''")
                    logger.debug("[price] cleaned=0")
                    price = 0

                try:
                    imageUrl = extract_image_url(card, IMAGE_SELECTOR, BASE_URL)
                except:
                    imageUrl = None

                specs = extract_specs(raw_title)
                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(href or "").lower(),
                    price=price,
                    imageUrl=imageUrl,
                    ram_gb=specs["ram_gb"],
                    storage_gb=specs["storage_gb"],
                    color_raw=specs["color_raw"],
                    model_code=specs["model_code"],
                ))
                logger.debug(f"+ [{brand}] {title}")

                if limit is not None and len(phones) >= limit:
                    break

            except Exception as e:
                logger.warning(f"Skipped a card: {e}")

        if limit is not None and len(phones) >= limit:
            break

        page += 1

    driver.quit()

    if limit is None and len(seen_urls) < MIN_EXPECTED_PRODUCTS:
        logger.error(f"ananas found only {len(seen_urls)} products "
                     f"(expected at least {MIN_EXPECTED_PRODUCTS}) using selector "
                     f"{PRODUCT_CARD_SELECTOR!r} — it may no longer match the live markup.")

    return phones


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    logger.info("Starting ananas.mk phone scraper...")
    phones = scrape_all_phones()
    logger.info(f"Total phones scraped: {len(phones)}")
    for brand in BRANDS:
        count = len([p for p in phones if p.brand == brand])
        logger.info(f"  {brand}: {count} phones")
    for phone in phones:
        logger.debug(phone)