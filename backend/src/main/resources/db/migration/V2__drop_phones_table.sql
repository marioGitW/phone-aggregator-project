-- The old single-table "phones" rows predate spec extraction (no storage_gb/ram_gb),
-- so migrating them would collapse genuinely distinct storage/RAM variants into a
-- single phone_models row. Data is repopulated by re-running the scraper's import
-- against phones.json, which now carries spec-extracted fields.
DROP TABLE IF EXISTS phones;
