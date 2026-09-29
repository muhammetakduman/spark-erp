-- The usual supplier and purchase cost of a product, editable on the product
-- form. Each material line still records its own supplier and price; these
-- are the catalog defaults (e.g. for a free quote item added to the catalog).
-- The cost is a VAT-exclusive TL unit price, stored as text like every amount.

ALTER TABLE product ADD COLUMN supplier_name TEXT NULL;
ALTER TABLE product ADD COLUMN purchase_price TEXT NULL;
