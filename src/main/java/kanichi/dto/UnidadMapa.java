package kanichi.dto;

public record UnidadMapa(int numero, String titulo, Estado estado) {
  public enum Estado {
    COMPLETADA,
    ACTIVA,
    BLOQUEADA
  }

  public boolean isActiva() {
    return estado == Estado.ACTIVA;
  }

  public boolean isBloqueada() {
    return estado == Estado.BLOQUEADA;
  }

  public String claseCss() {
    return "unidad--" + estado.name().toLowerCase();
  }
}