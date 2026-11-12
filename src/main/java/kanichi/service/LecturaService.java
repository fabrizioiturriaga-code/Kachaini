package kanichi.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.ActividadPractica;
import kanichi.dto.ResultadoLectura;
import kanichi.dto.ResultadoLectura.PreguntaCorregida;
import kanichi.dto.ResultadoLectura.ResumenTema;
import kanichi.dto.UnidadMapa;
import kanichi.model.Estudiante;
import kanichi.model.Lectura;
import kanichi.model.Lectura.PreguntaLectura;
import kanichi.model.Lectura.Tema;
import kanichi.repository.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LecturaService {
  private final EstudianteRepository estudianteRepository;
  private final MapaMisionesService mapaMisionesService;

  public LecturaService(
      EstudianteRepository estudianteRepository, MapaMisionesService mapaMisionesService) {
    this.estudianteRepository = estudianteRepository;
    this.mapaMisionesService = mapaMisionesService;
  }

  @Transactional(readOnly = true)
  public List<ActividadPractica> listar(Long estudianteId) {
    Map<Integer, UnidadMapa> unidades =
        obtenerUnidades(estudianteId).stream()
            .collect(Collectors.toMap(UnidadMapa::numero, Function.identity()));
    return Arrays.stream(Lectura.values())
        .filter(lectura -> unidades.containsKey(lectura.getUnidad()))
        .map(
            lectura -> {
              UnidadMapa unidad = unidades.get(lectura.getUnidad());
              return new ActividadPractica(
                  lectura.getCodigo(),
                  lectura.getTitulo(),
                  unidad.numero(),
                  unidad.titulo(),
                  !unidad.isBloqueada());
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean estaDisponible(Long estudianteId, Lectura lectura) {
    return obtenerUnidades(estudianteId).stream()
        .filter(unidad -> unidad.numero() == lectura.getUnidad())
        .findFirst()
        .map(unidad -> !unidad.isBloqueada())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public ResultadoLectura corregir(Lectura lectura, List<Integer> respuestas) {
    List<PreguntaLectura> preguntas = lectura.getPreguntas();
    if (respuestas == null
        || respuestas.size() < preguntas.size()
        || respuestas.subList(0, preguntas.size()).stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Responde todas las preguntas.");
    }

    List<PreguntaCorregida> corregidas = new ArrayList<>();
    Map<Tema, int[]> conteo = new EnumMap<>(Tema.class);
    for (int i = 0; i < preguntas.size(); i++) {
      PreguntaLectura pregunta = preguntas.get(i);
      Integer respuesta = respuestas.get(i);
      boolean correcta = pregunta.acepta(respuesta);
      corregidas.add(
          new PreguntaCorregida(pregunta, pregunta.opcion(respuesta).orElse("-"), correcta));
      conteo.computeIfAbsent(pregunta.tema(), tema -> new int[2])[correcta ? 0 : 1]++;
    }

    List<ResumenTema> temas =
        conteo.entrySet().stream()
            .map(e -> new ResumenTema(e.getKey().getNombre(), e.getValue()[0], e.getValue()[1]))
            .toList();
    return new ResultadoLectura(List.copyOf(corregidas), temas);
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
