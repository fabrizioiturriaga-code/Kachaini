ALTER TABLE retos_docente
    ADD COLUMN pregunta VARCHAR(300),
    ADD COLUMN opciones VARCHAR(700),
    ADD COLUMN respuesta_correcta INTEGER,
    ADD CONSTRAINT ck_retos_docente_pregunta CHECK (
        (pregunta IS NULL AND opciones IS NULL AND respuesta_correcta IS NULL)
        OR (pregunta IS NOT NULL AND opciones IS NOT NULL AND respuesta_correcta BETWEEN 0 AND 3)
    );

ALTER TABLE notificaciones
    ADD COLUMN fallida TIMESTAMP WITH TIME ZONE;