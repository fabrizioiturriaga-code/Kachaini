package kanichi.validation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PoliticaCorreo {
  private final Set<String> dominiosPermitidos = new HashSet<>();

  public PoliticaCorreo(String listaDominios) {
    this(listaDominios, false);
  }

  @Autowired
  public PoliticaCorreo(
      @Value("${kaichaini.institutional-domains:}") String listaDominios,
      @Value("${kaichaini.institutional-domains-required:false}") boolean dominiosObligatorios) {
    for (String dominio : listaDominios.split(",")) {
      if (!dominio.isBlank()) {
        dominiosPermitidos.add(normalizar(dominio));
      }
    }
    if (dominiosObligatorios && dominiosPermitidos.isEmpty()) {
      throw new IllegalArgumentException(
          "Configura los dominios institucionales antes de iniciar en producción.");
    }
  }

  public static String normalizar(String correo) {
    if (correo == null) {
      return "";
    }
    return correo.strip().toLowerCase(Locale.ROOT);
  }

  public boolean permite(String correo) {
    if (dominiosPermitidos.isEmpty()) {
      return true;
    }
    String dominio = correo.substring(correo.lastIndexOf('@') + 1);
    return dominiosPermitidos.contains(dominio);
  }
}
