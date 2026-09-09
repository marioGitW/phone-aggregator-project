import logging
import re
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, clean_price, extract_image_url
from utils.spec_extractor import extract_specs

BASE_URL = "https://setec.mk"
CATEGORY_URL = (
    "https://setec.mk/category/mobilni-20telefoni-67?"
    "sort=%D0%9D%D0%B0%D1%98%D0%B5%D0%B2%D1%82%D0%B8%D0%BD%D0%BE"
    "&minPrice=995"
    "&maxPrice=164990"
    "&%D0%91%D1%80%D0%B5%D0%BD%D0%B4=Apple"
    "&%D0%91%D1%80%D0%B5%D0%BD%D0%B4=Samsung"
    "&%D0%91%D1%80%D0%B5%D0%BD%D0%B4=Honor"
    "&%D0%91%D1%80%D0%B5%D0%BD%D0%B4=Xiaomi"
)
IMAGE_SELECTOR = "img.object-cover"
PRODUCT_CARD_SELECTOR = "div[data-component='ProductCard']"
BRANDS = ["apple", "samsung", "xiaomi", "honor"]

logger = logging.getLogger(__name__)

# Roughly half of this source's typical full-run yield. A full run (no
# --limit) coming in under this means PRODUCT_CARD_SELECTOR likely stopped
# matching the live markup — log loudly instead of silently importing a
# partial dataset. (setec's frontend has already broken this selector once,
# via a Tailwind class rename — see git history.)
MIN_EXPECTED_PRODUCTS = 132

NAME_PREFIXES = [
    "преднарачка -",
    "мобилен телефон",
    "паметен телефон",
    "mobile phone",
    "smartphone"
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
                f"model_code={self.model_code}), imageUrl={self.imageUrl}")


def get_driver():
    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    driver = webdriver.Chrome(options=options)
    return driver


def wait_for_cards(driver, timeout=15):
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located(
                (By.CSS_SELECTOR, PRODUCT_CARD_SELECTOR)
            )
        )
        time.sleep(2)
        return True
    except:
        return False


def detect_brand_from_raw(raw_title):
    return get_brand_from_raw(raw_title)


def clean_name(name):
    name = name.lower().strip()
    for prefix in NAME_PREFIXES:
        name = name.replace(prefix, "").strip()
    name = re.sub(r'\(.*?\)', '', name).strip()
    name = re.sub(r'\s+', ' ', name).strip()
    return name


def format_title_setec(raw_title, brand):

    if not raw_title:
        return ""

    tokens = raw_title.lower().strip().split()
    
    def is_storage_token(token):
        return "gb" in token or "tb" in token

    if brand == "samsung":
        keep = []
        for token in tokens[:4]:
            if not is_storage_token(token):
                keep.append(token)
        return " ".join(keep).strip()

    return " ".join([t for t in tokens if not is_storage_token(t)]).strip()

def scrape_all_phones(limit=None):
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        url = f"{CATEGORY_URL}&page={page}"
        logger.info(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            logger.info(f"No products on page {page}, stopping.")
            break

        products = driver.find_elements(
            By.CSS_SELECTOR,
            PRODUCT_CARD_SELECTOR
        )

        logger.info(f"Found {len(products)} products on page {page}")

        if not products:
            break

        for p in products:
            try:
                raw_name = p.find_element(By.TAG_NAME, "h3").text.strip()
                link = p.find_element(By.TAG_NAME, "a").get_attribute("href")

                if link in seen_urls:
                    continue
                seen_urls.add(link)

                raw_title = remove_voucher(raw_name).lower()

                brand = get_brand_from_raw(raw_title)
                if brand is None or brand not in BRANDS:
                    continue

                title = format_title_setec(clean_name(raw_title), brand)

                try:
                    price_container = p.find_element(
                        By.XPATH,
                        ".//p[contains(., 'Клуб цена')]"
                    )
                    price_text = price_container.find_elements(By.TAG_NAME, "span")[1].text
                except:
                    price_text = p.find_element(
                        By.XPATH,
                        ".//p[contains(., 'Редовна цена')]"
                    ).text

                logger.debug(f"[price] raw={price_text!r}")
                price = clean_price(price_text)
                logger.debug(f"[price] cleaned={price}")
                if not price_text:
                    logger.debug("[price] raw=''")
                    logger.debug("[price] cleaned=0")

                try:
                    imageUrl = extract_image_url(p, IMAGE_SELECTOR, BASE_URL)
                except:
                    imageUrl = None

                specs = extract_specs(raw_title)
                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(link or "").lower(),
                    price=price,
                    imageUrl=imageUrl,
                    ram_gb=specs["ram_gb"],
                    storage_gb=specs["storage_gb"],
                    color_raw=specs["color_raw"],
                    model_code=specs["model_code"],
                ))

                logger.debug(f"+ [{brand}] {title} - {price}")

                if limit is not None and len(phones) >= limit:
                    break

            except Exception as e:
                logger.warning(f"Skipped a product: {e}")

        if limit is not None and len(phones) >= limit:
            break

        page += 1

    driver.quit()

    if limit is None and len(seen_urls) < MIN_EXPECTED_PRODUCTS:
        logger.error(f"setec found only {len(seen_urls)} products "
                     f"(expected at least {MIN_EXPECTED_PRODUCTS}) using selector "
                     f"{PRODUCT_CARD_SELECTOR!r} — it may no longer match the live markup.")

    return phones


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    logger.info("Starting setec.mk phone scraper...")
    phones = scrape_all_phones()

    logger.info(f"Total phones scraped: {len(phones)}")
    for brand in BRANDS:
        count = len([p for p in phones if p.brand == brand])
        logger.info(f"  {brand}: {count} phones")
    for phone in phones:
        logger.debug(phone)