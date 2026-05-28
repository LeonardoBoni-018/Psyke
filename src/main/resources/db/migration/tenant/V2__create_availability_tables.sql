CREATE TABLE IF NOT EXISTS rooms (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id  UUID NOT NULL,
    name       VARCHAR(200) NOT NULL,
    color      VARCHAR(50),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS professional_availability (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id  UUID NOT NULL,
    day_of_week      INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    session_duration INTEGER NOT NULL DEFAULT 50,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_prof_day UNIQUE (professional_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS availability_exceptions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id  UUID NOT NULL,
    exception_date   DATE NOT NULL,
    start_time       TIME,
    end_time         TIME,
    type             VARCHAR(30) NOT NULL DEFAULT 'MANUAL_BLOCK',
    reason           VARCHAR(500),
    active           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS slots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id  UUID NOT NULL,
    start_time       TIMESTAMPTZ NOT NULL,
    end_time         TIMESTAMPTZ NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'FREE',
    patient_id       UUID,
    room_id          UUID,
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slots_professional_time  ON slots(professional_id, start_time);
CREATE INDEX IF NOT EXISTS idx_slots_professional_status ON slots(professional_id, status);
CREATE INDEX IF NOT EXISTS idx_avail_professional       ON professional_availability(professional_id);
CREATE INDEX IF NOT EXISTS idx_exceptions_professional  ON availability_exceptions(professional_id, exception_date);
CREATE INDEX IF NOT EXISTS idx_rooms_clinic             ON rooms(clinic_id);
