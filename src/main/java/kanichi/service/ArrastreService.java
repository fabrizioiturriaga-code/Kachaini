package kanichi.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kanichi.dto.ActividadPractica;
import kanichi.dto.ResultadoArrastre;
import kanichi.dto.ResultadoArrastre.Emparejamiento;
import kanichi.dto.UnidadMapa;
import kanichi.model.ActividadArrastre;
import kanichi.model.ActividadArrastre.Pareja;
import kanichi.model.Estudiante;
import kanichi.repository.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArrastreService {
  private final EstudianteRepository estudianteRepository;
  private final MapaMisionesService mapaMisionesService;

  public ArrastreService(
      EstudianteRepository estudianteRepository, MapaMisionesService mapaMisionesService) {
    this.estudianteRepository = estudianteRepository;
    this.mapaMisionesService = mapaMisionesService;
  }

  @Transactional(readOnly = true)
  public List<ActividadPractica> listar(Long estudianteId) {
    Map<Integer, UnidadMapa> unidades =
        obtenerUnidades(estudianteId).stream()
            .collect(Collectors.toMap(UnidadMapa::numero, Function.identity()));
    return Arrays.stream(ActividadArrastre.values())
        .filter(actividad -> unidades.containsKey(actividad.getUnidad()))
        .map(
            actividad -> {
              UnidadMapa unidad = unidades.get(actividad.getUnidad());
              return new ActividadPractica(
                  actividad.getCodigo(),
                  actividad.getTitulo(),
                  unidad.numero(),
                  unidad.titulo(),
                  !unidad.isBloqueada());
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean estaDisponible(Long estudianteId, ActividadArrastre actividad) {
    return obtenerUnidades(estudianteId).stream()
        .filter(unidad -> unidad.numero() == actividad.getUnidad())
        .findFirst()
        .map(unidad -> !unidad.isBloqueada())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public ResultadoArrastre corregir(ActividadArrastre actividad, List<Integer> asignaciones) {
    List<Pareja> parejas = actividad.getParejas();
    if (asignaciones == null
        || asignaciones.size() < parejas.size()
        || asignaciones.subList(0, parejas.size()).stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Ubica todos los elementos.");
    }

    List<Emparejamiento> emparejamientos =
        IntStream.range(0, parejas.size())
            .mapToObj(
                i -> {
                  int elegido = asignaciones.get(i);
                  String texto =
                      elegido >= 0 && elegido < parejas.size()
                          ? parejas.get(elegido).elemento()
                          : "-";
                  return new Emparejamiento(parejas.get(i).destino(), texto, elegido == i);
                })
            .toList();
    return new ResultadoArrastre(emparejamientos);
  }

  private List<UnidadMapa> obtenerUnidades(Long estudianteId) {
    Estudiante estudiante =
        estudianteRepository
            .findById(estudianteId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));
    return mapaMisionesService.obtenerMapa(
        estudiante.getGrado(),
        estudiante.isOnboardingCompletado(),
        estudiante.getLeccionesCompletadas());
  }
}
