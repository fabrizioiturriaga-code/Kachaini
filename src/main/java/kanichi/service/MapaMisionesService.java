package kanichi.service;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadMapa.Estado;
import kanichi.model.Leccion;
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
    return obtenerMapa(moduloInicialCompletado, Set.of());
  }

  /**
   * HU-18: estado de cada unidad según las lecciones completadas. Una unidad queda completada
   * cuando todas sus lecciones lo están; la siguiente se desbloquea automáticamente.
   */
  public List<UnidadMapa> obtenerMapa(boolean moduloInicialCompletado, Set<Leccion> completadas) {
    return IntStream.rangeClosed(1, UNIDADES.size())
        .mapToObj(
            numero -> {
              int hechas = completadasEn(numero, completadas);
              int total = Leccion.deUnidad(numero).size();
              boolean anteriorCompletada =
                  numero == 1 || completadasEn(numero - 1, completadas) == total(numero - 1);
              Estado estado;
              if (!moduloInicialCompletado || !anteriorCompletada) {
                estado = Estado.BLOQUEADA;
              } else if (hechas == total) {
                estado = Estado.COMPLETADA;
              } else {
                estado = Estado.ACTIVA;
              }
              return new UnidadMapa(numero, UNIDADES.get(numero - 1), estado, hechas, total);
            })
        .toList();
  }

  public int totalUnidades() {
    return UNIDADES.size();
  }

  public String tituloDeUnidad(int numero) {
    return UNIDADES.get(numero - 1);
  }

  public String primeraUnidad() {
    return UNIDADES.get(0);
  }

  private static int completadasEn(int unidad, Set<Leccion> completadas) {
    return (int) Leccion.deUnidad(unidad).stream().filter(completadas::contains).count();
  }

  private static int total(int unidad) {
    return Leccion.deUnidad(unidad).size();
  }
}
