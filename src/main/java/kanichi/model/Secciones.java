package kanichi.model;

import java.util.List;

public final class Secciones {
  public static final List<String> DISPONIBLES = List.of("A", "B", "C");

  private Secciones() {}

  public static boolean esValida(String seccion) {
    return seccion != null && DISPONIBLES.contains(seccion);
  }
}
