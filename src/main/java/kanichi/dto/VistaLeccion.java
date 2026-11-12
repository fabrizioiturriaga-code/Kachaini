package kanichi.dto;

import kanichi.model.Leccion;

/** Lección a mostrar al estudiante, con su estado y su energía actual. */
public record VistaLeccion(
    Leccion leccion, boolean completada, boolean disponible, EstadoEnergia energia) {}
