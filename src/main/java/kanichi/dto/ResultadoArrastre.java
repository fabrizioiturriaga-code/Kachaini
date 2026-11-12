package kanichi.dto;

import java.util.List;

public record ResultadoArrastre(List<Emparejamiento> emparejamientos) {

  public record Emparejamiento(String destino, String elegido, boolean correcto) {}

  public int aciertos() {
    return (int) emparejamientos.stream().filter(Emparejamiento::correcto).count();
  }

  public int total() {
    return emparejamientos.size();
  }

  public boolean isPerfecto() {
    return aciertos() == total();
  }

  public boolean esCorrecto(int indice) {
    return emparejamientos.get(indice).correcto();
  }

  public String claseDestino(int indice) {
    return esCorrecto(indice) ? "arrastre__destino--correcto" : "arrastre__destino--incorrecto";
  }
}
