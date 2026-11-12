package kanichi.dto;

public record NivelEstudiante(int numero, int xpTotal) {
  public static final int XP_POR_NIVEL = 100;

  public static NivelEstudiante de(int xpTotal) {
    int xp = Math.max(0, xpTotal);
    return new NivelEstudiante(xp / XP_POR_NIVEL + 1, xp);
  }

  public int xpEnNivel() {
    return xpTotal % XP_POR_NIVEL;
  }

  public int xpParaSiguiente() {
    return XP_POR_NIVEL - xpEnNivel();
  }

  public int xpPorNivel() {
    return XP_POR_NIVEL;
  }
}
