package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadMapa.Estado;
import kanichi.dto.UnidadesDocente.EnMapa;
import kanichi.dto.UnidadesDocente.EstadoLeccion;
import kanichi.dto.UnidadesDocente.VistaLeccion;
import kanichi.dto.UnidadesDocente.VistaUnidad;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.LeccionDocente;
import kanichi.model.Logro;
import kanichi.model.UnidadDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.LeccionDocenteRepository;
import kanichi.repository.UnidadDocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeccionDocenteService {
  private final UnidadDocenteRepository unidadRepository;
  private final LeccionDocenteRepository leccionRepository;
  private final EstudianteRepository estudianteRepository;
  private final CalculadoraXp calculadoraXp;
  private final Clock reloj;

  @Autowired
  public LeccionDocenteService(
      UnidadDocenteRepository unidadRepository,
      LeccionDocenteRepository leccionRepository,
      EstudianteRepository estudianteRepository,
      CalculadoraXp calculadoraXp) {
    this(
        unidadRepository, leccionRepository, estudianteRepository, calculadoraXp, Clock.systemUTC());
  }

  public LeccionDocenteService(
      UnidadDocenteRepository unidadRepository,
      LeccionDocenteRepository leccionRepository,
      EstudianteRepository estudianteRepository,
      CalculadoraXp calculadoraXp,
      Clock reloj) {
    this.unidadRepository = unidadRepository;
    this.leccionRepository = leccionRepository;
    this.estudianteRepository = estudianteRepository;
    this.calculadoraXp = calculadoraXp;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public List<EnMapa> unidadesDelMapa(Long estudianteId) {
    Estudiante estudiante = buscar(estudianteId);
    return mapaDe(estudiante, leccionRepository.completadasPor(estudianteId));
  }

  @Transactional(readOnly = true)
  public VistaUnidad obtenerUnidad(Long estudianteId, Long unidadId) {
    Estudiante estudiante = buscar(estudianteId);
    Set<Long> completadas = leccionRepository.completadasPor(estudianteId);
    EnMapa unidad = enMapa(estudiante, completadas, unidadId);
    List<LeccionDocente> lecciones = leccionRepository.findByUnidadIdOrderByOrdenAsc(unidadId);
    List<EstadoLeccion> estados = new ArrayList<>();
    for (int i = 0; i < lecciones.size(); i++) {
      LeccionDocente leccion = lecciones.get(i);
      estados.add(
          new EstadoLeccion(
              leccion,
              completadas.contains(leccion.getId()),
              disponible(estudiante, lecciones, i, completadas)));
    }
    return new VistaUnidad(unidad, estados, estudiante.estadoEnergia(reloj.instant()));
  }

  @Transactional(readOnly = true)
  public VistaLeccion obtenerLeccion(Long estudianteId, Long leccionId) {
    Estudiante estudiante = buscar(estudianteId);
    LeccionDocente leccion = leccionDeSuGrado(estudiante, leccionId);
    Set<Long> completadas = leccionRepository.completadasPor(estudianteId);
    List<LeccionDocente> lecciones =
        leccionRepository.findByUnidadIdOrderByOrdenAsc(leccion.getUnidadId());
    return new VistaLeccion(
        leccion,
        enMapa(estudiante, completadas, leccion.getUnidadId()),
        completadas.contains(leccion.getId()),
        disponible(estudiante, lecciones, lecciones.indexOf(leccion), completadas),
        estudiante.estadoEnergia(reloj.instant()));
  }

  @Transactional
  public ResultadoLeccion responder(
      Long estudianteId, Long leccionId, Integer respuesta, UUID intento) {
    Estudiante estudiante =
        estudianteRepository
            .bloquearPorId(estudianteId)
            .orElseThrow(LeccionDocenteService::noEncontrado);
    Instant ahora = reloj.instant();
    estudiante.recargarEnergia(ahora);

    if (!estudiante.isOnboardingCompletado()) {
      throw new MisionNoPermitidaException("Completa primero el módulo inicial.");
    }
    LeccionDocente leccion = leccionDeSuGrado(estudiante, leccionId);
    Set<Long> completadas = new HashSet<>(leccionRepository.completadasPor(estudianteId));
    if (completadas.contains(leccionId)) {
      return resultado(Resultado.YA_COMPLETADA, 0, estudiante, ahora, false, List.of());
    }
    List<LeccionDocente> lecciones =
        leccionRepository.findByUnidadIdOrderByOrdenAsc(leccion.getUnidadId());
    if (!disponible(estudiante, lecciones, lecciones.indexOf(leccion), completadas)) {
      throw new MisionNoPermitidaException("Completa primero la lección anterior.");
    }
    if (estudiante.estadoEnergia(ahora).isAgotada()) {
      return resultado(Resultado.SIN_ENERGIA, 0, estudiante, ahora, false, List.of());
    }
    boolean correcta = leccion.aceptaRespuesta(respuesta);
    if (estudianteRepository.registrarIntento(estudianteId, intento, correcta) == 0) {
      return resultado(Resultado.INTENTO_REPETIDO, 0, estudiante, ahora, false, List.of());
    }
    if (!correcta) {
      estudiante.perderEnergia(ahora);
      return resultado(Resultado.INCORRECTA, 0, estudiante, ahora, false, List.of());
    }

    leccionRepository.registrarCompletada(estudianteId, leccionId, ahora);
    int xp = calculadoraXp.calcular(estudianteId, leccion.getXp(), ahora);
    estudiante.sumarXp(xp);
    estudianteRepository.registrarXp(estudianteId, xp, ahora);
    List<Logro> logros = EvaluadorLogros.otorgarNuevos(estudiante, ahora);
    completadas.add(leccionId);
    boolean unidadCompletada =
        lecciones.stream().allMatch(l -> completadas.contains(l.getId()));
    return resultado(Resultado.CORRECTA, xp, estudiante, ahora, unidadCompletada, logros);
  }

  private List<EnMapa> mapaDe(Estudiante estudiante, Set<Long> completadas) {
    List<UnidadDocente> unidades = unidadRepository.findByGradoOrderByIdAsc(estudiante.getGrado());
    if (unidades.isEmpty()) {
      return List.of();
    }
    Map<Long, List<LeccionDocente>> lecciones =
        leccionRepository
            .findByUnidadIdInOrderByUnidadIdAscOrdenAsc(
                unidades.stream().map(UnidadDocente::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(LeccionDocente::getUnidadId));
    int numero = Leccion.unidadesDelGrado(estudiante.getGrado()).size();
    List<EnMapa> mapa = new ArrayList<>();
    for (UnidadDocente unidad : unidades) {
      List<LeccionDocente> deLaUnidad = lecciones.getOrDefault(unidad.getId(), List.of());
      if (deLaUnidad.isEmpty()) {
        continue;
      }
      int hechas = (int) deLaUnidad.stream().filter(l -> completadas.contains(l.getId())).count();
      Estado estado;
      if (!estudiante.isOnboardingCompletado()) {
        estado = Estado.BLOQUEADA;
      } else if (hechas == deLaUnidad.size()) {
        estado = Estado.COMPLETADA;
      } else {
        estado = Estado.ACTIVA;
      }
      mapa.add(
          new EnMapa(
              unidad.getId(),
              new UnidadMapa(++numero, unidad.getTitulo(), estado, hechas, deLaUnidad.size())));
    }
    return mapa;
  }

  private EnMapa enMapa(Estudiante estudiante, Set<Long> completadas, Long unidadId) {
    return mapaDe(estudiante, completadas).stream()
        .filter(unidad -> unidad.id().equals(unidadId))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private LeccionDocente leccionDeSuGrado(Estudiante estudiante, Long leccionId) {
    LeccionDocente leccion =
        leccionRepository
            .findById(leccionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    unidadRepository
        .findById(leccion.getUnidadId())
        .filter(unidad -> estudiante.getGrado() != null && unidad.getGrado() == estudiante.getGrado())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return leccion;
  }

  private static boolean disponible(
      Estudiante estudiante, List<LeccionDocente> lecciones, int indice, Set<Long> completadas) {
    return estudiante.isOnboardingCompletado()
        && indice >= 0
        && (indice == 0 || completadas.contains(lecciones.get(indice - 1).getId()));
  }

  private static ResultadoLeccion resultado(
      Resultado resultado,
      int xp,
      Estudiante estudiante,
      Instant ahora,
      boolean unidadCompletada,
      List<Logro> logros) {
    return new ResultadoLeccion(
        resultado,
        xp,
        estudiante.estadoEnergia(ahora),
        unidadCompletada,
        null,
        estudiante.rachaEn(ahora).dias(),
        false,
        logros);
  }

  private Estudiante buscar(Long estudianteId) {
    return estudianteRepository.findById(estudianteId).orElseThrow(LeccionDocenteService::noEncontrado);
  }

  private static IllegalStateException noEncontrado() {
    return new IllegalStateException("No se encontró el estudiante autenticado.");
  }
}
