-- V16: store uploaded files in the database (container disks are ephemeral on PaaS hosts)
--      and add indexes for the dashboard, calendar, report and search queries.

CREATE TABLE IF NOT EXISTS document_contents (
    document_id UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    content     BYTEA NOT NULL
);

ALTER TABLE documents ALTER COLUMN storage_path DROP NOT NULL;

-- Calendar / dashboard / reports: appointments of a clinic in a time window.
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_start
    ON appointments(clinic_id, start_time);

-- Patient history ordered by date.
CREATE INDEX IF NOT EXISTS idx_appointments_patient_start
    ON appointments(patient_id, start_time DESC);

-- Patient list ordered by name and filtered by status.
CREATE INDEX IF NOT EXISTS idx_patients_clinic_status_name
    ON patients(clinic_id, status, full_name);

-- Name search (LOWER(full_name) LIKE ...): prefix searches can use this index.
CREATE INDEX IF NOT EXISTS idx_patients_lower_name
    ON patients(clinic_id, LOWER(full_name) text_pattern_ops);

-- Invoice listing (newest first) and period reports.
CREATE INDEX IF NOT EXISTS idx_invoices_clinic_created
    ON invoices(clinic_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_clinic_status_paid
    ON invoices(clinic_id, status, payment_date);
