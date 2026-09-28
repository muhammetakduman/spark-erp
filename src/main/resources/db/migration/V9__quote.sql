-- The user's own company (single row, id = 1): letterhead of quote and job PDFs.
CREATE TABLE company (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    name TEXT NULL,
    slogan TEXT NULL,
    address TEXT NULL,
    phone TEXT NULL,
    email TEXT NULL,
    web TEXT NULL,
    tax_office TEXT NULL,
    tax_no TEXT NULL,
    logo BLOB NULL
);

-- Written price quotes ("Teklif"). Prices are VAT-exclusive; VAT is added on
-- top of the subtotal. job_id is set once an accepted quote is turned into a job.
CREATE TABLE quote (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quote_no TEXT NOT NULL,
    quote_date TEXT NOT NULL,
    validity_days INTEGER NOT NULL DEFAULT 15 CHECK (validity_days >= 0),
    customer_id INTEGER NULL,
    company_name TEXT NOT NULL,
    address TEXT NULL,
    contact_person TEXT NULL,
    phone TEXT NULL,
    fax TEXT NULL,
    email TEXT NULL,
    subject TEXT NULL,
    discount_type TEXT NOT NULL DEFAULT 'NONE' CHECK (discount_type IN ('NONE', 'AMOUNT', 'PERCENT')),
    discount_value TEXT NULL,
    labor_amount TEXT NULL,
    vat_rate INTEGER NULL DEFAULT 20 CHECK (vat_rate IN (1, 10, 20)),
    notes TEXT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED')),
    prepared_by_name TEXT NULL,
    prepared_by_title TEXT NULL,
    created_at TEXT NOT NULL,
    job_id INTEGER NULL,
    CONSTRAINT fk_quote_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE SET NULL,
    CONSTRAINT fk_quote_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_quote_no ON quote (quote_no);
CREATE INDEX idx_quote_date ON quote (quote_date);
CREATE INDEX idx_quote_customer_id ON quote (customer_id);
CREATE INDEX idx_quote_job_id ON quote (job_id);

-- Quote lines in the order the user arranged them (line_no). A line is either
-- a catalog product or a free text item (e.g. "Sarf malzeme", 1 × lump sum).
CREATE TABLE quote_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quote_id INTEGER NOT NULL,
    line_no INTEGER NOT NULL,
    product_id INTEGER NULL,
    free_product_name TEXT NULL,
    brand TEXT NULL,
    quantity TEXT NOT NULL,
    unit TEXT NOT NULL CHECK (unit IN ('PIECE', 'METER', 'KG', 'SET', 'PACKAGE')),
    unit_price TEXT NOT NULL,
    description TEXT NULL,
    CONSTRAINT fk_quote_item_quote FOREIGN KEY (quote_id) REFERENCES quote (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT,
    CONSTRAINT ck_quote_item_product_or_name
        CHECK (product_id IS NOT NULL OR (free_product_name IS NOT NULL AND TRIM(free_product_name) <> ''))
);

CREATE INDEX idx_quote_item_quote_id ON quote_item (quote_id);
CREATE INDEX idx_quote_item_product_id ON quote_item (product_id);
