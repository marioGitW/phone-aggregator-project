import re
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, normalize_price

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

BRANDS = ["apple", "samsung", "xiaomi", "honor"]

NAME_PREFIXES = [
    "преднарачка -",
    "мобилен телефон",
    "паметен телефон",
    "mobile phone",
    "smartphone"
]

class Phone:

    def __init__(self, brand, title, rawTitle, siteLink, price):
        self.brand = brand
        self.title = title
        self.rawTitle = rawTitle
        self.siteLink = siteLink
        self.price = price

    def __repr__(self):
        return (f"Phone(brand={self.brand}, title={self.title}, rawTitle={self.rawTitle}, "
                f"siteLink={self.siteLink}, price={self.price})")


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
                (By.CSS_SELECTOR, "div.relative.bg-white.p-4.rounded-\\[20px\\]")
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

def scrape_all_phones():
    driver = get_driver()
    phones = []
    seen_urls = set()
    page = 1

    while True:
        url = f"{CATEGORY_URL}&page={page}"
        print(f"Scraping page {page} -> {url}")
        driver.get(url)

        loaded = wait_for_cards(driver)
        if not loaded:
            print(f"No products on page {page}, stopping.")
            break

        products = driver.find_elements(
            By.CSS_SELECTOR,
            "div.relative.bg-white.p-4.rounded-\\[20px\\]"
        )

        print(f"Found {len(products)} products on page {page}")

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

                price = normalize_price(price_text)

                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(link or "").lower(),
                    price=price
                ))

                print(f"  + [{brand}] {title} – {price}")

            except Exception as e:
                print("  Skipped a product:", e)

        page += 1

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting setec.mk phone scraper...\n")
    phones = scrape_all_phones()

    print(f"\n{'='*50}")
    print(f"Total phones scraped: {len(phones)}")
    print(f"{'='*50}")
    for brand in BRANDS:
        count = len([p for p in phones if p.brand == brand])
        print(f"  {brand}: {count} phones")
    print()
    for phone in phones:
        print(phone)