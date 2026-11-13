package kanichi.dto;

public record ResumenAvisos(int nuevos, int porVencer) {
  public static final ResumenAvisos VACIO = new ResumenAvisos(0, 0);

  public boolean hayPendientes() {
    return nuevos > 0 || porVencer > 0;
  }

  public String texto() {
    StringBuilder texto = new StringBuilder("Tienes ");
    if (nuevos > 0) {
      texto.append(nuevos).append(nuevos == 1 ? " reto nuevo" : " retos nuevos");
    }
    if (nuevos > 0 && porVencer > 0) {
      texto.append(" y ");
    }
    if (porVencer > 0) {
      texto.append(porVencer).append(porVencer == 1 ? " reto próximo" : " retos próximos");
      texto.append(" a vencer");
    }
    return texto.append(".").toString();
  }
}