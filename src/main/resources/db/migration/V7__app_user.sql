-- Application users. The table starts empty: on the first start the app asks
-- for the first administrator (initial setup screen).
-- role: ADMIN = owner (sees everything), MANAGER = operator (no cost, profit,
-- receivable, payment or wage figures).
CREATE TABLE app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    title TEXT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MANAGER')),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL
);

CREATE UNIQUE INDEX uq_app_user_username ON app_user (username COLLATE NOCASE);
