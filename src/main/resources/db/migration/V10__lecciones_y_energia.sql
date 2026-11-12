-- HU-12: energía de aprendizaje. HU-18: progreso por lección para el mapa de aprendizaje.
ALTER TABLE estudiantes
    ADD COLUMN energia INTEGER NOT NULL DEFAULT 5 CHECK (energia BETWEEN 0 AND 5);

ALTER TABLE estudiantes
    ADD COLUMN energia_actualizada TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE lecciones_completadas (
    estudiante_id BIGINT NOT NULL REFERENCES estudiantes(id) ON DELETE CASCADE,
    leccion VARCHAR(40) NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (estudiante_id, leccion)
);
