-- Attendance rewrite: every worked day is its own row; day_factor is 1.0
-- (full day) or 0.5 (half day), so "days worked" is always SUM(day_factor).
ALTER TABLE attendance ADD COLUMN day_factor TEXT NOT NULL DEFAULT '1.0';
ALTER TABLE attendance ADD COLUMN note TEXT NULL;

-- The unique (job_id, employee_id, attendance_date) constraint already exists
-- since V1 (uq_attendance_job_employee_date). The single-column indexes are
-- replaced by composite ones matching the per-employee / per-job date queries.
DROP INDEX IF EXISTS idx_attendance_employee_id;
DROP INDEX IF EXISTS idx_attendance_job_id;
CREATE INDEX idx_attendance_employee_date ON attendance (employee_id, attendance_date);
CREATE INDEX idx_attendance_job_date ON attendance (job_id, attendance_date);
