package kanichi.model;

public enum FactorXp {
  X1_5("1.5x", 1.5),
  X2("2x", 2),
  X3("3x", 3);

  private final String etiqueta;
  private final double valor;

  FactorXp(String etiqueta, double valor) {
    this.etiqueta = etiqueta;
    this.valor = valor;
  }

  public int aplicar(int xp) {
    return (int) Math.round(xp * valor);
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public double getValor() {
    return valor;
  }
}
