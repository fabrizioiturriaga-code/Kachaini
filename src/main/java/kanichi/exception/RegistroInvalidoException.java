package kanichi.exception;

import java.util.Map;

public class RegistroInvalidoException extends RuntimeException {
  private final Map<String, String> errores;

  public RegistroInvalidoException(Map<String, String> errores) {
    super("Registro inválido");
    this.errores = Map.copyOf(errores);
  }

  public Map<String, String> getErrores() {
    return errores;
  }
}
