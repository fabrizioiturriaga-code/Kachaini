package kanichi.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kanichi.model.Liga;

public record RankingSalon(
    int grado,
    String seccion,
    List<Posicion> posiciones,
    long miPosicion,
    int miXp,
    int xpSemanaPasada,
    int xpTotal,
    LocalDate inicioSemana,
    LocalDate finSemana) {

  private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");

  
  public RankingSalon(
      int grado, String seccion, List<Posicion> posiciones, long miPosicion, int miXp) {
    this(grado, seccion, posiciones, miPosicion, miXp, 0, miXp, null, null);
  }


  public String rangoSemana() {
    if (inicioSemana == null || finSemana == null) {
      return "";
    }
    return FORMATO_DIA.format(inicioSemana) + " al " + FORMATO_DIA.format(finSemana);
  }

  public Liga miLiga() {
    return Liga.para(miXp);
  }

  public Liga siguienteLiga() {
    return miLiga().siguiente().orElse(null);
  }

  public int xpParaSiguienteLiga() {
    Liga siguiente = siguienteLiga();
    return siguiente == null ? 0 : siguiente.getXpMinima() - miXp;
  }

  public record Posicion(int puesto, String nombre, String avatar, int xp, boolean esActual) {
    public Liga liga() {
      return Liga.para(xp);
    }
  }
}
