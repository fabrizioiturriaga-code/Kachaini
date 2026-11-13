package kanichi.model;

public enum TipoPublicacion {

  MENSAJE,
  RETO_SALON,
  RACHA;

  public boolean isAutomatica() {
    return this != MENSAJE;
  }
}