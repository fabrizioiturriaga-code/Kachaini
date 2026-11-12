package kanichi.dto;

import java.util.List;

/** Mapa de aprendizaje de un estudiante junto con su energía actual. */
public record MapaAprendizaje(List<UnidadMapa> unidades, EstadoEnergia energia) {}
