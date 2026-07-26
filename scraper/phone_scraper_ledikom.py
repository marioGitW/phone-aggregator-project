import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from utils.phone_utils import remove_voucher, get_brand_from_raw, format_title, normalize_price

BASE_URL = "https://ledikom.mk"

BRAND_URLS = {
    "apple": "https://ledikom.mk/c/416/telefoni/apple-iphone",
    "samsung": "https://ledikom.mk/c/421/telefoni/samsung",
    "xiaomi": "https://ledikom.mk/c/424/telefoni/xiaomi",
    "google": "https://ledikom.mk/c/413/telefoni/google",
    "honor": "https://ledikom.mk/c/411/telefoni/honor",
    "oneplus": "https://ledikom.mk/c/441/telefoni/oneplus",
}

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
    return webdriver.Chrome(options=options)


def wait_for_products(driver, timeout=15):
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((By.CLASS_NAME, "item-in-grid"))
        )
        time.sleep(2)
        return True
    except:
        return False


def scrape_ledikom():
    driver = get_driver()
    phones = []
    seen_urls = set()

    for brand, url in BRAND_URLS.items():
        print(f"\nScraping {brand} -> {url}")
        driver.get(url)

        loaded = wait_for_products(driver)
        if not loaded:
            print(f"No products found for {brand}")
            continue

        products = driver.find_elements(By.CLASS_NAME, "item-in-grid")
        print(f"Found {len(products)} {brand} phones")

        for p in products:
            try:
                raw_title = remove_voucher(p.find_element(By.CSS_SELECTOR, ".item-name a").text.strip()).lower()
                brand = get_brand_from_raw(raw_title)
                title = format_title(raw_title, brand)

                try:
                    price_text = p.find_element(By.CSS_SELECTOR, ".grid-new-price").text
                except:
                    price_text = p.find_element(By.CSS_SELECTOR, ".price").text

                price = normalize_price(price_text)

                link = p.find_element(By.CSS_SELECTOR, "a[href*='/p/']").get_attribute("href")

                if link in seen_urls:
                    continue
                seen_urls.add(link)

                phones.append(Phone(
                    brand=brand,
                    title=title,
                    rawTitle=raw_title,
                    siteLink=(link or "").lower(),
                    price=price,
                ))

                print(f"  + {title} – {price}")

            except Exception as e:
                print("  Skipped product:", e)

    driver.quit()
    return phones


if __name__ == "__main__":
    print("Starting Ledikom scraper...\n")
    phones = scrape_ledikom()

    print(f"\n{'='*50}")
    print(f"Total phones scraped: {len(phones)}")
    print(f"{'='*50}")
    for brand in BRAND_URLS.keys():
        count = len([p for p in phones if p.brand == brand])
        print(f"  {brand}: {count} phones")
    print()
    for phone in phones:
        print(phone)