ALTER TABLE estudiantes
    ADD COLUMN avatar VARCHAR(20) NOT NULL DEFAULT 'KAI';

ALTER TABLE estudiantes
    ADD CONSTRAINT ck_estudiantes_avatar
    CHECK (avatar IN ('KAI', 'ZORRO', 'COLIBRI', 'PUMA', 'CONDOR'));
