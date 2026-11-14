CREATE TABLE administradores (
    id BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE
);

ALTER TABLE docentes_autorizaciones ADD COLUMN administrador_id BIGINT;

CREATE OR REPLACE FUNCTION registrar_autorizacion_docente() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO docentes_autorizaciones (docente_id, autorizado, administrador_id)
    VALUES (
        NEW.id,
        NEW.autorizado,
        NULLIF(current_setting('kaichaini.administrador_id', true), '')::BIGINT
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
