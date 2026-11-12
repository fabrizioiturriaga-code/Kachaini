package kanichi.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
          "Independencia del Perú",
          "Primeros años de la República y la era del guano",
          "La Guerra del Pacífico y la Reconstrucción",
          "El Perú en el siglo XX",
          "El Perú contemporáneo");

  public List<UnidadMapa> obtenerMapa(int grado, boolean moduloInicialCompletado) {
    return obtenerMapa(grado, moduloInicialCompletado, Set.of());
  }

  /**
   * HU-18: estado de cada unidad según las lecciones completadas. Una unidad queda completada
   * cuando todas sus lecciones lo están; la siguiente se desbloquea automáticamente.
   */
  public List<UnidadMapa> obtenerMapa(
      int grado, boolean moduloInicialCompletado, Set<Leccion> completadas) {
    List<Integer> unidades = Leccion.unidadesDelGrado(grado);
    return unidades.stream()
        .map(
            numero -> {
              int hechas = completadasEn(numero, completadas);
              int total = total(numero);
              boolean anteriorCompletada =
                  numero.equals(unidades.get(0))
                      || completadasEn(numero - 1, completadas) == total(numero - 1);
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

  public Optional<UnidadMapa> buscarUnidad(
      int grado, boolean moduloInicialCompletado, Set<Leccion> completadas, int numero) {
    return obtenerMapa(grado, moduloInicialCompletado, completadas).stream()
        .filter(unidad -> unidad.numero() == numero)
        .findFirst();
  }

  public int totalUnidades() {
    return UNIDADES.size();
  }

  public String tituloDeUnidad(int numero) {
    return UNIDADES.get(numero - 1);
  }

  public boolean esUltimaDelGrado(int numero) {
    List<Integer> unidades = Leccion.unidadesDelGrado(Leccion.gradoDeUnidad(numero));
    return numero == unidades.get(unidades.size() - 1);
  }

  private static int completadasEn(int unidad, Set<Leccion> completadas) {
    return (int) Leccion.deUnidad(unidad).stream().filter(completadas::contains).count();
  }

  private static int total(int unidad) {
    return Leccion.deUnidad(unidad).size();
  }
}
