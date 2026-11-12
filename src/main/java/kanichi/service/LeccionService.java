package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import kanichi.dto.MapaAprendizaje;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.UnidadMapa;
import kanichi.dto.VistaLeccion;
import kanichi.dto.VistaUnidad;
import kanichi.dto.VistaUnidad.EstadoLeccion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-18: mapa de aprendizaje con lecciones por unidad. HU-12: energía que baja con cada error y
 * se recupera con el tiempo.
 */
@Service
public class LeccionService {
  private final EstudianteRepository estudianteRepository;
  private final MapaMisionesService mapaMisionesService;
  private final Clock reloj;

  @Autowired
  public LeccionService(
      EstudianteRepository estudianteRepository, MapaMisionesService mapaMisionesService) {
    this(estudianteRepository, mapaMisionesService, Clock.systemUTC());
  }

  public LeccionService(
      EstudianteRepository estudianteRepository,
      MapaMisionesService mapaMisionesService,
      Clock reloj) {
    this.estudianteRepository = estudianteRepository;
    this.mapaMisionesService = mapaMisionesService;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public MapaAprendizaje obtenerMapa(Long estudianteId) {
    Estudiante estudiante = buscar(estudianteId);
    return new MapaAprendizaje(
        mapaMisionesService.obtenerMapa(
            estudiante.isOnboardingCompletado(), estudiante.getLeccionesCompletadas()),
        estudiante.estadoEnergia(reloj.instant()));
  }

  /** Detalle de la unidad {@code numero} (de 1 a {@link MapaMisionesService#totalUnidades()}). */
  @Transactional(readOnly = true)
  public VistaUnidad obtenerUnidad(Long estudianteId, int numero) {
    Estudiante estudiante = buscar(estudianteId);
    UnidadMapa unidad =
        mapaMisionesService
            .obtenerMapa(estudiante.isOnboardingCompletado(), estudiante.getLeccionesCompletadas())
            .get(numero - 1);
    List<EstadoLeccion> lecciones =
        Leccion.deUnidad(numero).stream()
            .map(
                l ->
                    new EstadoLeccion(
                        l, estudiante.haCompletado(l), estaDisponible(estudiante, l)))
            .toList();
    return new VistaUnidad(unidad, lecciones, estudiante.estadoEnergia(reloj.instant()));
  }

  @Transactional(readOnly = true)
  public VistaLeccion obtenerLeccion(Long estudianteId, Leccion leccion) {
    Estudiante estudiante = buscar(estudianteId);
    return new VistaLeccion(
        leccion,
        estudiante.haCompletado(leccion),
        estaDisponible(estudiante, leccion),
        estudiante.estadoEnergia(reloj.instant()));
  }

  /**
   * Evalúa la respuesta del estudiante. Una respuesta incorrecta resta una unidad de energía; con
   * la energía agotada no se puede responder hasta que se recupere. La respuesta correcta otorga
   * la XP de la lección y, si completa la unidad, desbloquea la siguiente.
   */
  @Transactional
  public ResultadoLeccion responder(Long estudianteId, Leccion leccion, Integer respuesta) {
    Estudiante estudiante =
        estudianteRepository.bloquearPorId(estudianteId).orElseThrow(LeccionService::noEncontrado);
    Instant ahora = reloj.instant();
    estudiante.recargarEnergia(ahora);

    if (!estudiante.isOnboardingCompletado()) {
      throw new MisionNoPermitidaException("Completa primero el módulo inicial.");
    }
    if (estudiante.haCompletado(leccion)) {
      return new ResultadoLeccion(
          Resultado.YA_COMPLETADA, 0, estudiante.estadoEnergia(ahora), false, null);
    }
    if (!estaDisponible(estudiante, leccion)) {
      throw new MisionNoPermitidaException("Completa primero la lección anterior.");
    }
    if (estudiante.estadoEnergia(ahora).isAgotada()) {
      return new ResultadoLeccion(
          Resultado.SIN_ENERGIA, 0, estudiante.estadoEnergia(ahora), false, null);
    }
    if (!leccion.aceptaRespuesta(respuesta)) {
      estudiante.perderEnergia(ahora);
      return new ResultadoLeccion(
          Resultado.INCORRECTA, 0, estudiante.estadoEnergia(ahora), false, null);
    }

    estudiante.registrarLeccion(leccion);
    boolean unidadCompletada =
        Leccion.deUnidad(leccion.getUnidad()).stream().allMatch(estudiante::haCompletado);
    Integer desbloqueada =
        unidadCompletada && leccion.getUnidad() < mapaMisionesService.totalUnidades()
            ? leccion.getUnidad() + 1
            : null;
    return new ResultadoLeccion(
        Resultado.CORRECTA,
        leccion.getXp(),
        estudiante.estadoEnergia(ahora),
        unidadCompletada,
        desbloqueada);
  }

  /** Las lecciones se completan en orden y solo después del módulo inicial. */
  private static boolean estaDisponible(Estudiante estudiante, Leccion leccion) {
    return estudiante.isOnboardingCompletado()
        && leccion.anterior().map(estudiante::haCompletado).orElse(true);
  }

  private Estudiante buscar(Long estudianteId) {
    return estudianteRepository.findById(estudianteId).orElseThrow(LeccionService::noEncontrado);
  }

  private static IllegalStateException noEncontrado() {
    return new IllegalStateException("No se encontró el estudiante autenticado.");
  }
}
