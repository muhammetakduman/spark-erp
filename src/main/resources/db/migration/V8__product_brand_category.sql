-- Brand and category on products. The same product name may now exist once
-- per brand ("1,5mm NYA Kablo" by ÖZNUR and by HES are two products), so the
-- UNIQUE(name) constraint from V1 is replaced by a unique (name, brand) index.
-- Category comes back on purpose (dropped in V2): it is needed for the
-- per-category supplier price comparison.
--
-- SQLite cannot drop a column constraint, so the table is rebuilt. It is the
-- parent of material_item (ON DELETE RESTRICT); with deferred foreign keys the
-- temporary drop is only checked at commit, when every product row is back
-- with its original id. Existing products get no brand or category.

PRAGMA defer_foreign_keys = ON;

CREATE TABLE product_backup AS SELECT id, name, unit FROM product;

DROP TABLE product;

CREATE TABLE product (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    unit TEXT NOT NULL CHECK (unit IN ('PIECE', 'METER', 'KG', 'SET', 'PACKAGE')),
    brand TEXT NULL,
    category TEXT NULL
);

INSERT INTO product (id, name, unit) SELECT id, name, unit FROM product_backup;

DROP TABLE product_backup;

CREATE UNIQUE INDEX uq_product_name_brand ON product (name COLLATE NOCASE, COALESCE(brand, '') COLLATE NOCASE);
CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_brand ON product (brand);
