-- Daily work plan ("İş Takip / Günlük Program"): where the team goes on a
-- day, whether they went, and what happens if they did not. A job that was
-- moved to another day keeps its row (status POSTPONED) and the new row points
-- back to it through source_daily_job_id, so postponements can be counted.
CREATE TABLE daily_job (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_date TEXT NOT NULL,
    job_id INTEGER NULL,
    title TEXT NOT NULL,
    customer_id INTEGER NULL,
    customer_name TEXT NULL,
    address TEXT NULL,
    phone TEXT NULL,
    status TEXT NOT NULL DEFAULT 'PLANNED'
        CHECK (status IN ('PLANNED', 'COMPLETED', 'NOT_VISITED', 'POSTPONED', 'CANCELLED')),
    priority TEXT NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('NORMAL', 'URGENT')),
    time_of_day TEXT NULL,
    note TEXT NULL,
    completion_note TEXT NULL,
    postponed_to TEXT NULL,
    source_daily_job_id INTEGER NULL,
    created_by_user_id INTEGER NULL,
    created_at TEXT NOT NULL,
    CONSTRAINT fk_daily_job_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE SET NULL,
    CONSTRAINT fk_daily_job_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE SET NULL,
    CONSTRAINT fk_daily_job_source FOREIGN KEY (source_daily_job_id) REFERENCES daily_job (id) ON DELETE SET NULL,
    CONSTRAINT fk_daily_job_user FOREIGN KEY (created_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_daily_job_date ON daily_job (job_date);
CREATE INDEX idx_daily_job_status ON daily_job (status);
CREATE INDEX idx_daily_job_job_id ON daily_job (job_id);
CREATE INDEX idx_daily_job_source ON daily_job (source_daily_job_id);

-- Who goes to a daily job.
CREATE TABLE daily_job_employee (
    daily_job_id INTEGER NOT NULL,
    employee_id INTEGER NOT NULL,
    PRIMARY KEY (daily_job_id, employee_id),
    CONSTRAINT fk_daily_job_employee_job FOREIGN KEY (daily_job_id) REFERENCES daily_job (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_job_employee_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE
);

CREATE INDEX idx_daily_job_employee_employee ON daily_job_employee (employee_id);
