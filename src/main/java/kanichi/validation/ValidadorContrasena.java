package kanichi.validation;

import java.nio.charset.StandardCharsets;

public final class ValidadorContrasena {
  private ValidadorContrasena() {}

  public static boolean esValida(String valor) {
    return valor != null
        && valor.length() >= 8
        && valor.getBytes(StandardCharsets.UTF_8).length <= 72
        && valor.codePoints().anyMatch(Character::isLetter)
        && valor.codePoints().anyMatch(Character::isDigit);
  }
}
