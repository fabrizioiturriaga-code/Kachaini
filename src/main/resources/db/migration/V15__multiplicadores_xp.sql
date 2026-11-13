CREATE TABLE multiplicadores_xp (
    estudiante_id BIGINT PRIMARY KEY REFERENCES estudiantes(id) ON DELETE CASCADE,
    docente_id BIGINT NOT NULL REFERENCES docentes(id) ON DELETE CASCADE,
    factor VARCHAR(10) NOT NULL CHECK (factor IN ('X1_5', 'X2', 'X3')),
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fin TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (fin > inicio)
);

CREATE INDEX ix_multiplicadores_xp_fin ON multiplicadores_xp (fin);
