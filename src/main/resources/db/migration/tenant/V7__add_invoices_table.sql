-- V7: tabela de faturamento por sessão

CREATE TABLE IF NOT EXISTS invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    appointment_id  UUID REFERENCES appointments(id) ON DELETE SET NULL,
    patient_id      UUID NOT NULL,
    professional_id UUID NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(50),
    payment_date    TIMESTAMPTZ,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoices_clinic ON invoices(clinic_id);
CREATE INDEX IF NOT EXISTS idx_invoices_patient ON invoices(patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
