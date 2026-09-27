-- Simplification pass: drop the supplier master table (replaced by a free-text
-- name on the material line) and drop the unused product category field.

ALTER TABLE material_item ADD COLUMN supplier_name TEXT;

UPDATE material_item
SET supplier_name = (SELECT s.name FROM supplier s WHERE s.id = material_item.supplier_id)
WHERE supplier_id IS NOT NULL;

-- SQLite cannot DROP COLUMN a column that is part of a foreign key definition
-- on the same table, so material_item is rebuilt without supplier_id/its FK.
CREATE TABLE material_item_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    item_date TEXT,
    quantity TEXT NOT NULL,
    purchase_unit_price TEXT,
    supplier_name TEXT,
    sale_unit_price TEXT NOT NULL,
    vat_rate INTEGER CHECK (vat_rate IN (1, 10, 20)),
    note TEXT,
    CONSTRAINT fk_material_item_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT fk_material_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT
);

INSERT INTO material_item_new (id, job_id, product_id, item_date, quantity, purchase_unit_price,
        supplier_name, sale_unit_price, vat_rate, note)
SELECT id, job_id, product_id, item_date, quantity, purchase_unit_price,
        supplier_name, sale_unit_price, vat_rate, note
FROM material_item;

DROP TABLE material_item;
ALTER TABLE material_item_new RENAME TO material_item;

CREATE INDEX idx_material_item_job_id ON material_item (job_id);
CREATE INDEX idx_material_item_product_id ON material_item (product_id);

DROP TABLE supplier;

ALTER TABLE product DROP COLUMN category;
