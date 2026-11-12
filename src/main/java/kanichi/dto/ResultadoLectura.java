package kanichi.dto;

import java.util.List;
import kanichi.model.Lectura.PreguntaLectura;

public record ResultadoLectura(List<PreguntaCorregida> preguntas, List<ResumenTema> temas) {

  public record PreguntaCorregida(
      PreguntaLectura pregunta, String respuestaElegida, boolean correcta) {}

  public record ResumenTema(String tema, int aciertos, int errores) {}

  public int aciertos() {
    return (int) preguntas.stream().filter(PreguntaCorregida::correcta).count();
  }

  public int total() {
    return preguntas.size();
  }

  public boolean isPerfecto() {
    return aciertos() == total();
  }
}
