package kanichi.service;

import java.util.List;
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

  public List<UnidadMapa> obtenerMapa(Long estudianteId) {
    return java.util.stream.IntStream.range(0, UNIDADES.size())
        .mapToObj(
            i -> new UnidadMapa(i + 1, UNIDADES.get(i), i == 0 ? Estado.ACTIVA : Estado.BLOQUEADA))
        .toList();
  }
}