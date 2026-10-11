-- first_appointment / last_appointment were never populated. They are now maintained when a session
-- is completed; this backfills the history for sessions that were already marked DONE.
UPDATE patients p
SET first_appointment = v.first_visit,
    last_appointment  = v.last_visit
FROM (
    SELECT patient_id, MIN(start_time) AS first_visit, MAX(start_time) AS last_visit
    FROM appointments
    WHERE status = 'DONE'
    GROUP BY patient_id
) v
WHERE p.id = v.patient_id
  AND p.last_appointment IS NULL;
