package kanichi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class RespuestasForm {
  private List<Integer> respuestas = new ArrayList<>();

  public List<Integer> getRespuestas() {
    return respuestas;
  }

  public void setRespuestas(List<Integer> respuestas) {
    this.respuestas = respuestas == null ? new ArrayList<>() : respuestas;
  }

  public Integer respuesta(int indice) {
    return indice >= 0 && indice < respuestas.size() ? respuestas.get(indice) : null;
  }

  public boolean estaCompleto(int total) {
    return respuestas.size() >= total && IntStream.range(0, total).allMatch(i -> respuesta(i) != null);
  }
}
