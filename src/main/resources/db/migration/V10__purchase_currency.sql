-- Purchases in foreign currency. purchase_unit_price keeps the amount as
-- entered (in purchase_currency); purchase_exchange_rate is the TL value of
-- one unit of that currency on the purchase day; purchase_unit_price_tl is
-- price × rate, frozen with the line so later rate changes never alter past
-- costs or profits. Every cost, profit and report uses the TL value.
ALTER TABLE material_item ADD COLUMN purchase_currency TEXT NOT NULL DEFAULT 'TRY'
    CHECK (purchase_currency IN ('TRY', 'USD', 'EUR'));
ALTER TABLE material_item ADD COLUMN purchase_exchange_rate TEXT NULL;
ALTER TABLE material_item ADD COLUMN purchase_unit_price_tl TEXT NULL;

-- Every existing purchase was entered in lira.
UPDATE material_item
SET purchase_exchange_rate = '1',
    purchase_unit_price_tl = purchase_unit_price;
