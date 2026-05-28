CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS clinics (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    cnpj       VARCHAR(18) UNIQUE,
    crp        VARCHAR(20),
    address    TEXT,
    phone      VARCHAR(20),
    logo_url   VARCHAR(500),
    settings   JSONB DEFAULT '{}',
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS professionals (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id         UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    user_id           UUID,
    crp               VARCHAR(20) NOT NULL,
    full_name         VARCHAR(300) NOT NULL,
    specialty         VARCHAR(200),
    approach          VARCHAR(200),
    resume            TEXT,
    session_value     DECIMAL(10,2),
    session_duration  INTEGER NOT NULL DEFAULT 50,
    accepts_insurance BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS patients (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id        UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    full_name        VARCHAR(300) NOT NULL,
    birth_date       DATE,
    cpf              VARCHAR(14),
    gender           VARCHAR(30),
    marital_status   VARCHAR(30),
    occupation       VARCHAR(200),
    phone            VARCHAR(20),
    email            VARCHAR(255),
    insurance        VARCHAR(100),
    insurance_number VARCHAR(50),
    referred_by      VARCHAR(200),
    status           VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    first_appointment TIMESTAMPTZ,
    last_appointment TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_professionals_clinic ON professionals(clinic_id);
CREATE INDEX IF NOT EXISTS idx_patients_clinic ON patients(clinic_id);
