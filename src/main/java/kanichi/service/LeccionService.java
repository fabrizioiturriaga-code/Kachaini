package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kanichi.dto.MapaAprendizaje;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.UnidadMapa;
import kanichi.dto.VistaLeccion;
import kanichi.dto.VistaUnidad;
import kanichi.dto.VistaUnidad.EstadoLeccion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.EntregaDesarrollo;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Logro;
import kanichi.repository.EntregaDesarrolloRepository;
import kanichi.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-18: mapa de aprendizaje con lecciones por unidad. HU-12: energía que baja con cada error y se
 * recupera con el tiempo. HU-13: logros al completar lecciones. HU-14: registra la XP con su fecha
 * para el ranking semanal.
 */
@Service
public class LeccionService {
  private final EstudianteRepository estudianteRepository;
  private final EntregaDesarrolloRepository entregaRepository;
  private final MapaMisionesService mapaMisionesService;
  private final Clock reloj;
  private final CalculadoraXp calculadoraXp;

  public LeccionService(
      EstudianteRepository estudianteRepository,
      EntregaDesarrolloRepository entregaRepository,
      MapaMisionesService mapaMisionesService) {
    this(estudianteRepository, entregaRepository, mapaMisionesService, Clock.systemUTC());
  }

  @Autowired
  public LeccionService(
      EstudianteRepository estudianteRepository,
      EntregaDesarrolloRepository entregaRepository,
      MapaMisionesService mapaMisionesService,
      CalculadoraXp calculadoraXp) {
    this(
        estudianteRepository,
        entregaRepository,
        mapaMisionesService,
        Clock.systemUTC(),
        calculadoraXp);
  }

  public LeccionService(
      EstudianteRepository estudianteRepository,
      EntregaDesarrolloRepository entregaRepository,
      MapaMisionesService mapaMisionesService,
      Clock reloj) {
    this(
        estudianteRepository,
        entregaRepository,
        mapaMisionesService,
        reloj,
        CalculadoraXp.SIN_MULTIPLICADOR);
  }

  public LeccionService(
      EstudianteRepository estudianteRepository,
      EntregaDesarrolloRepository entregaRepository,
      MapaMisionesService mapaMisionesService,
      Clock reloj,
      CalculadoraXp calculadoraXp) {
    this.estudianteRepository = estudianteRepository;
    this.entregaRepository = entregaRepository;
    this.mapaMisionesService = mapaMisionesService;
    this.reloj = reloj;
    this.calculadoraXp = calculadoraXp;
  }

  @Transactional(readOnly = true)
  public MapaAprendizaje obtenerMapa(Long estudianteId) {
    Estudiante estudiante = buscar(estudianteId);
    Instant ahora = reloj.instant();
    return new MapaAprendizaje(
        mapaMisionesService.obtenerMapa(
            estudiante.getGrado(),
            estudiante.isOnboardingCompletado(),
            estudiante.getLeccionesCompletadas()),
        estudiante.estadoEnergia(ahora),
        estudiante.rachaEn(ahora).dias());
  }

  /** Detalle de la unidad {@code numero} (de 1 a {@link MapaMisionesService#totalUnidades()}). */
  @Transactional(readOnly = true)
  public VistaUnidad obtenerUnidad(Long estudianteId, int numero) {
    Estudiante estudiante = buscar(estudianteId);
    UnidadMapa unidad =
        mapaMisionesService
            .buscarUnidad(
                estudiante.getGrado(),
                estudiante.isOnboardingCompletado(),
                estudiante.getLeccionesCompletadas(),
                numero)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    List<EstadoLeccion> lecciones =
        Leccion.deUnidad(numero).stream()
            .map(
                l ->
                    new EstadoLeccion(l, estudiante.haCompletado(l), estaDisponible(estudiante, l)))
            .toList();
    return new VistaUnidad(unidad, lecciones, estudiante.estadoEnergia(reloj.instant()));
  }

  @Transactional(readOnly = true)
  public VistaLeccion obtenerLeccion(Long estudianteId, Leccion leccion) {
    Estudiante estudiante = buscar(estudianteId);
    if (!esDeSuGrado(estudiante, leccion)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return new VistaLeccion(
        leccion,
        estudiante.haCompletado(leccion),
        estaDisponible(estudiante, leccion),
        estudiante.estadoEnergia(reloj.instant()));
  }

  @Transactional(readOnly = true)
  public Optional<EntregaDesarrollo> obtenerEntrega(Long estudianteId, Leccion leccion) {
    if (leccion.getPreguntaDesarrollo() == null) {
      return Optional.empty();
    }
    return entregaRepository.findByEstudianteIdAndLeccion(estudianteId, leccion.getCodigo());
  }

  @Transactional
  public boolean entregarRespuesta(Long estudianteId, Leccion leccion, String texto) {
    if (leccion.getPreguntaDesarrollo() == null
        || texto == null
        || texto.isBlank()
        || texto.length() > EntregaDesarrollo.MAXIMO_TEXTO) {
      throw new IllegalArgumentException("Respuesta de desarrollo inválida.");
    }
    Estudiante estudiante =
        estudianteRepository.bloquearPorId(estudianteId).orElseThrow(LeccionService::noEncontrado);
    if (!estaDisponible(estudiante, leccion)) {
      throw new MisionNoPermitidaException("Completa primero la lección anterior.");
    }
    if (obtenerEntrega(estudianteId, leccion).isPresent()) {
      return false;
    }
    entregaRepository.save(
        new EntregaDesarrollo(estudianteId, leccion.getCodigo(), texto, reloj.instant()));
    return true;
  }

  /**
   * Evalúa la respuesta del estudiante. Una respuesta incorrecta resta una unidad de energía; con
   * la energía agotada no se puede responder hasta que se recupere. La respuesta correcta otorga la
   * XP de la lección y, si completa la unidad, desbloquea la siguiente.
   */
  @Transactional
  public ResultadoLeccion responder(
      Long estudianteId, Leccion leccion, Integer respuesta, UUID intento) {
    Estudiante estudiante =
        estudianteRepository.bloquearPorId(estudianteId).orElseThrow(LeccionService::noEncontrado);
    Instant ahora = reloj.instant();
    estudiante.recargarEnergia(ahora);

    if (!estudiante.isOnboardingCompletado()) {
      throw new MisionNoPermitidaException("Completa primero el módulo inicial.");
    }
    if (!esDeSuGrado(estudiante, leccion)) {
      throw new MisionNoPermitidaException("Esta lección no corresponde a tu grado.");
    }
    if (estudiante.haCompletado(leccion)) {
      return new ResultadoLeccion(
          Resultado.YA_COMPLETADA, 0, estudiante.estadoEnergia(ahora), false, null, 0, false);
    }
    if (!estaDisponible(estudiante, leccion)) {
      throw new MisionNoPermitidaException("Completa primero la lección anterior.");
    }
    if (estudiante.estadoEnergia(ahora).isAgotada()) {
      return new ResultadoLeccion(
          Resultado.SIN_ENERGIA, 0, estudiante.estadoEnergia(ahora), false, null, 0, false);
    }
    boolean correcta = leccion.aceptaRespuesta(respuesta);
    if (estudianteRepository.registrarIntento(estudianteId, intento, correcta) == 0) {
      return new ResultadoLeccion(
          Resultado.INTENTO_REPETIDO, 0, estudiante.estadoEnergia(ahora), false, null, 0, false);
    }
    if (!correcta) {
      estudiante.perderEnergia(ahora);
      return new ResultadoLeccion(
          Resultado.INCORRECTA, 0, estudiante.estadoEnergia(ahora), false, null, 0, false);
    }

    boolean rachaAumentada = !estudiante.rachaEn(ahora).completadaHoy();
    estudiante.registrarLeccion(leccion, ahora);
    int xpGanada = calculadoraXp.calcular(estudianteId, leccion.getXp(), ahora);
    if (xpGanada > leccion.getXp()) {
      estudiante.sumarXp(xpGanada - leccion.getXp());
    }
    estudianteRepository.registrarXp(estudianteId, xpGanada, ahora);
    List<Logro> logrosNuevos = EvaluadorLogros.otorgarNuevos(estudiante, ahora);
    boolean unidadCompletada =
        Leccion.deUnidad(leccion.getUnidad()).stream().allMatch(estudiante::haCompletado);
    Integer desbloqueada =
        unidadCompletada && !mapaMisionesService.esUltimaDelGrado(leccion.getUnidad())
            ? leccion.getUnidad() + 1
            : null;
    return new ResultadoLeccion(
        Resultado.CORRECTA,
        xpGanada,
        estudiante.estadoEnergia(ahora),
        unidadCompletada,
        desbloqueada,
        estudiante.rachaEn(ahora).dias(),
        rachaAumentada,
        logrosNuevos);
  }

  /** Las lecciones se completan en orden y solo después del módulo inicial. */
  private static boolean estaDisponible(Estudiante estudiante, Leccion leccion) {
    return estudiante.isOnboardingCompletado()
        && esDeSuGrado(estudiante, leccion)
        && leccion.anterior().map(estudiante::haCompletado).orElse(true);
  }

  private static boolean esDeSuGrado(Estudiante estudiante, Leccion leccion) {
    return estudiante.getGrado() != null && estudiante.getGrado() == leccion.getGrado();
  }

  private Estudiante buscar(Long estudianteId) {
    return estudianteRepository.findById(estudianteId).orElseThrow(LeccionService::noEncontrado);
  }

  private static IllegalStateException noEncontrado() {
    return new IllegalStateException("No se encontró el estudiante autenticado.");
  }
}
