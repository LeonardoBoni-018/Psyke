-- V6: índices faltando e campo title em evolucoes_clinicas

CREATE INDEX IF NOT EXISTS idx_prontuarios_updated_at
    ON prontuarios(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_evolucoes_created_at
    ON evolucoes_clinicas(created_at DESC);

ALTER TABLE evolucoes_clinicas
    ADD COLUMN IF NOT EXISTS title VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_appointments_clinic_id
    ON appointments(clinic_id);

CREATE INDEX IF NOT EXISTS idx_appointments_professional_time
    ON appointments(professional_id, start_time, end_time)
    WHERE status IN ('SCHEDULED', 'CONFIRMED');
