-- Customers
CREATE TABLE customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    tax_no TEXT,
    note TEXT
);

-- Site works and service jobs (distinguished by `type`)
CREATE TABLE job (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('SITE', 'SERVICE')),
    name TEXT,
    address TEXT,
    start_date TEXT,
    end_date TEXT,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    service_fee TEXT,
    labor_fee TEXT,
    payment_received INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    CONSTRAINT fk_job_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE RESTRICT
);

CREATE INDEX idx_job_customer_id ON job (customer_id);

-- Product catalog
CREATE TABLE product (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    unit TEXT NOT NULL CHECK (unit IN ('PIECE', 'METER', 'KG', 'SET', 'PACKAGE')),
    category TEXT
);

-- Suppliers
CREATE TABLE supplier (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT
);

-- Material lines used on a job, priced at the time they were recorded
CREATE TABLE material_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    item_date TEXT,
    quantity TEXT NOT NULL,
    purchase_unit_price TEXT,
    supplier_id INTEGER,
    sale_unit_price TEXT NOT NULL,
    vat_rate INTEGER CHECK (vat_rate IN (1, 10, 20)),
    note TEXT,
    CONSTRAINT fk_material_item_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT fk_material_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT,
    CONSTRAINT fk_material_item_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE RESTRICT
);

CREATE INDEX idx_material_item_job_id ON material_item (job_id);
CREATE INDEX idx_material_item_product_id ON material_item (product_id);
CREATE INDEX idx_material_item_supplier_id ON material_item (supplier_id);

-- Employees (the master himself is also recorded here)
CREATE TABLE employee (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    default_daily_wage TEXT NOT NULL,
    is_master INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1
);

-- Daily attendance/wage entries, wage copied from the employee's default at entry time
CREATE TABLE attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    employee_id INTEGER NOT NULL,
    attendance_date TEXT NOT NULL,
    daily_wage TEXT NOT NULL,
    CONSTRAINT fk_attendance_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_job_employee_date UNIQUE (job_id, employee_id, attendance_date)
);

CREATE INDEX idx_attendance_job_id ON attendance (job_id);
CREATE INDEX idx_attendance_employee_id ON attendance (employee_id);

-- Payments collected against a job (partial payments for site works)
CREATE TABLE payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    method TEXT NOT NULL CHECK (method IN ('CASH', 'TRANSFER', 'CHECK', 'CARD')),
    note TEXT,
    CONSTRAINT fk_payment_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_job_id ON payment (job_id);

-- Key-value application settings (backup folder, etc.)
CREATE TABLE setting (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT
);
