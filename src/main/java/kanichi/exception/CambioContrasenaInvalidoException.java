package kanichi.exception;

public class CambioContrasenaInvalidoException extends RuntimeException {
  private final String campo;

  public CambioContrasenaInvalidoException(String campo, String mensaje) {
    super(mensaje);
    this.campo = campo;
  }

  public String getCampo() {
    return campo;
  }
}
