ALTER TABLE estudiantes
    ADD COLUMN xp INTEGER NOT NULL DEFAULT 0 CHECK (xp >= 0);

CREATE TABLE misiones_completadas (
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE CASCADE,
    mision VARCHAR(40) NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (estudiante_id, mision)
);

CREATE TABLE logros_estudiante (
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE CASCADE,
    logro VARCHAR(40) NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (estudiante_id, logro)
);

CREATE INDEX ix_estudiantes_ranking ON estudiantes (grado, seccion, xp DESC);
