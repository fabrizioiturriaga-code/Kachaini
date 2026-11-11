package kanichi.service;

import java.util.List;
import java.util.stream.IntStream;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadMapa.Estado;
import org.springframework.stereotype.Service;

@Service
public class MapaMisionesService {
  private static final List<String> UNIDADES =
      List.of(
          "Poblamiento de América y del Perú",
          "Culturas preíncas",
          "El Tahuantinsuyo",
          "Conquista del Perú",
          "Virreinato del Perú",
          "Independencia del Perú");

  /**
   * Las unidades permanecen bloqueadas hasta completar el módulo inicial (HU-06); al completarlo
   * se desbloquea automáticamente la primera.
   */
  public List<UnidadMapa> obtenerMapa(boolean moduloInicialCompletado) {
    return IntStream.range(0, UNIDADES.size())
        .mapToObj(
            i ->
                new UnidadMapa(
                    i + 1,
                    UNIDADES.get(i),
                    i == 0 && moduloInicialCompletado ? Estado.ACTIVA : Estado.BLOQUEADA))
        .toList();
  }

  public String primeraUnidad() {
    return UNIDADES.get(0);
  }
}
