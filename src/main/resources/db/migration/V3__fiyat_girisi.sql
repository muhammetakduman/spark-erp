-- Lets a material line's sale price be entered either per unit or as a
-- ready-made line total (price_entry_type = UNIT|TOTAL).

ALTER TABLE material_item ADD COLUMN price_entry_type TEXT NOT NULL DEFAULT 'UNIT'
    CHECK (price_entry_type IN ('UNIT', 'TOTAL'));

ALTER TABLE material_item ADD COLUMN sale_total_amount TEXT NULL;
