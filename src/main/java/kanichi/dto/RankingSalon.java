package kanichi.dto;

import java.util.List;

/** Ranking por XP de los estudiantes del mismo grado y sección. */
public record RankingSalon(
    int grado, String seccion, List<Posicion> posiciones, long miPosicion, int miXp) {

  public record Posicion(int puesto, String nombre, String avatar, int xp, boolean esActual) {}
}
