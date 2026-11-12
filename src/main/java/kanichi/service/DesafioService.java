package kanichi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.PanelDesafios;
import kanichi.dto.PanelDesafios.Companero;
import kanichi.dto.PanelDesafios.Participante;
import kanichi.dto.PanelDesafios.VistaDesafio;
import kanichi.exception.DesafioInvalidoException;
import kanichi.model.Desafio;
import kanichi.model.Desafio.Estado;
import kanichi.model.Estudiante;
import kanichi.model.Logro;
import kanichi.model.TipoDesafio;
import kanichi.repository.ActividadRepository;
import kanichi.repository.ActividadRepository.Avance;
import kanichi.repository.DesafioRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.UsuarioRepository;
import kanichi.validation.PoliticaCorreo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesafioService {
  public static final int MAXIMO_ABIERTOS = 3;
  private static final Set<Estado> ABIERTOS = EnumSet.of(Estado.PENDIENTE, Estado.ACTIVO);
  private static final Set<Estado> VISIBLES =
      EnumSet.of(Estado.PENDIENTE, Estado.ACTIVO, Estado.COMPLETADO);
  private static final Duration MARGEN = Duration.ofMinutes(1);

  private final DesafioRepository desafioRepository;
  private final EstudianteRepository estudianteRepository;
  private final UsuarioRepository usuarioRepository;
  private final ActividadRepository actividadRepository;
  private final LigaService ligaService;
  private final EntityManager entityManager;
  private final Clock reloj;

  @Autowired
  public DesafioService(
      DesafioRepository desafioRepository,
      EstudianteRepository estudianteRepository,
      UsuarioRepository usuarioRepository,
      ActividadRepository actividadRepository,
      LigaService ligaService,
      EntityManager entityManager) {
    this(
        desafioRepository,
        estudianteRepository,
        usuarioRepository,
        actividadRepository,
        ligaService,
        entityManager,
        Clock.systemUTC());
  }

  public DesafioService(
      DesafioRepository desafioRepository,
      EstudianteRepository estudianteRepository,
      UsuarioRepository usuarioRepository,
      ActividadRepository actividadRepository,
      LigaService ligaService,
      EntityManager entityManager,
      Clock reloj) {
    this.desafioRepository = desafioRepository;
    this.estudianteRepository = estudianteRepository;
    this.usuarioRepository = usuarioRepository;
    this.actividadRepository = actividadRepository;
    this.ligaService = ligaService;
    this.entityManager = entityManager;
    this.reloj = reloj;
  }

  @Transactional
  public PanelDesafios panel(Long estudianteId) {
    Instant ahora = reloj.instant();
    List<Desafio> desafios = desafioRepository.deEstudiante(estudianteId, VISIBLES);

    List<String> notificaciones = new ArrayList<>();
    for (Desafio desafio : desafios) {
      if (desafio.getEstado() == Estado.ACTIVO && completarSiCumple(desafio, ahora)) {
        notificaciones.add(
            "🎉 ¡Completaron el desafío «"
                + desafio.getTipo().getNombre()
                + "»! Cada uno ganó "
                + desafio.getTipo().getXpRecompensa()
                + " XP y la insignia 🤝 Trabajo en equipo.");
      }
    }

    Estudiante yo = buscar(estudianteId);
    Set<Long> ids = new HashSet<>();
    desafios.forEach(
        d -> {
          ids.add(d.getCreadorId());
          ids.add(d.getCompaneroId());
        });
    Map<Long, Estudiante> participantes =
        estudianteRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Estudiante::getId, Function.identity()));

    List<VistaDesafio> invitaciones = new ArrayList<>();
    List<VistaDesafio> propios = new ArrayList<>();
    for (Desafio desafio : desafios) {
      VistaDesafio vista = vista(desafio, estudianteId, participantes, ahora);
      if (vista.soyInvitado() && desafio.getEstado() == Estado.PENDIENTE) {
        invitaciones.add(vista);
      } else {
        propios.add(vista);
      }
    }

    List<Companero> companeros =
        estudianteRepository.findByGradoAndSeccionOrderByNombreAsc(yo.getGrado(), yo.getSeccion())
            .stream()
            .filter(e -> !e.getId().equals(estudianteId))
            .map(e -> new Companero(e.getId(), e.getNombre(), e.getAvatar().getIcono()))
            .toList();

    return new PanelDesafios(
        actividadRepository.tieneActividad(estudianteId),
        companeros,
        invitaciones,
        propios,
        notificaciones);
  }

  @Transactional
  public void crear(Long estudianteId, Long companeroId, String correo, TipoDesafio tipo) {
    if (tipo == null) {
      throw new DesafioInvalidoException("Elige un tipo de desafío.");
    }
    if (!actividadRepository.tieneActividad(estudianteId)) {
      throw new DesafioInvalidoException(
          "Completa al menos una lección o práctica para crear desafíos.");
    }
    Long otroId = resolverCompanero(companeroId, correo);
    if (otroId.equals(estudianteId)) {
      throw new DesafioInvalidoException("No puedes desafiarte a ti mismo.");
    }
    if (desafioRepository.existeEntre(estudianteId, otroId, ABIERTOS)) {
      throw new DesafioInvalidoException(
          "Ya tienes un desafío pendiente o activo con ese estudiante.");
    }
    if (desafioRepository.deEstudiante(estudianteId, ABIERTOS).size() >= MAXIMO_ABIERTOS) {
      throw new DesafioInvalidoException(
          "Puedes tener como máximo " + MAXIMO_ABIERTOS + " desafíos pendientes o activos.");
    }
    desafioRepository.save(new Desafio(estudianteId, otroId, tipo, reloj.instant()));
  }

  @Transactional
  public void aceptar(Long estudianteId, Long desafioId) {
    invitacionPendiente(estudianteId, desafioId).aceptar(reloj.instant());
  }

  @Transactional
  public void rechazar(Long estudianteId, Long desafioId) {
    invitacionPendiente(estudianteId, desafioId).rechazar();
  }

  private Desafio invitacionPendiente(Long estudianteId, Long desafioId) {
    Desafio desafio =
        desafioRepository
            .bloquearPorId(desafioId)
            .filter(
                d -> d.getCompaneroId().equals(estudianteId) && d.getEstado() == Estado.PENDIENTE)
            .orElseThrow(
                () -> new DesafioInvalidoException("Esta invitación ya no está disponible."));
    return desafio;
  }

  private Long resolverCompanero(Long companeroId, String correo) {
    if (companeroId != null) {
      return estudianteRepository
          .findById(companeroId)
          .map(Estudiante::getId)
          .orElseThrow(() -> new DesafioInvalidoException("El compañero elegido no existe."));
    }
    if (correo != null && !correo.isBlank()) {
      return usuarioRepository
          .findByCorreo(PoliticaCorreo.normalizar(correo))
          .filter(Estudiante.class::isInstance)
          .map(u -> u.getId())
          .orElseThrow(
              () -> new DesafioInvalidoException("No encontramos a un estudiante con ese correo."));
    }
    throw new DesafioInvalidoException("Elige un compañero o escribe el correo de un estudiante.");
  }

  private boolean completarSiCumple(Desafio desafio, Instant ahora) {
    Avance total = avanceConjunto(desafio, ahora);
    TipoDesafio tipo = desafio.getTipo();
    if (!tipo.cumplido(total.tareas(), total.aciertos(), total.intentos())) {
      return false;
    }
    entityManager.refresh(desafio, LockModeType.PESSIMISTIC_WRITE);
    if (desafio.getEstado() != Estado.ACTIVO) {
      return false;
    }
    desafio.completar(ahora);
    List<Long> ids =
        List.of(desafio.getCreadorId(), desafio.getCompaneroId()).stream().sorted().toList();
    for (Long id : ids) {
      Estudiante participante = buscar(id);
      entityManager.refresh(participante, LockModeType.PESSIMISTIC_WRITE);
      participante.sumarXp(tipo.getXpRecompensa());
      estudianteRepository.registrarXp(id, tipo.getXpRecompensa(), ahora);
      participante.otorgarLogro(Logro.TRABAJO_EN_EQUIPO);
      EvaluadorLogros.otorgarNuevos(participante, ahora);
      ligaService.evaluar(participante, ahora, false);
    }
    return true;
  }

  private VistaDesafio vista(
      Desafio desafio, Long yoId, Map<Long, Estudiante> estudiantes, Instant ahora) {
    boolean sinIniciar = desafio.getEstado() == Estado.PENDIENTE;
    Avance creador = sinIniciar ? Avance.VACIO : avance(desafio, desafio.getCreadorId(), ahora);
    Avance companero =
        sinIniciar ? Avance.VACIO : avance(desafio, desafio.getCompaneroId(), ahora);
    Avance total = creador.mas(companero);
    return new VistaDesafio(
        desafio.getId(),
        desafio.getTipo(),
        desafio.getEstado(),
        List.of(
            participante(estudiantes.get(desafio.getCreadorId()), creador, yoId),
            participante(estudiantes.get(desafio.getCompaneroId()), companero, yoId)),
        total.tareas(),
        total.porcentaje(),
        desafio.getCompaneroId().equals(yoId));
  }

  private static Participante participante(Estudiante estudiante, Avance avance, Long yoId) {
    return new Participante(
        estudiante.getNombre(),
        estudiante.getAvatar().getIcono(),
        avance.tareas(),
        avance.porcentaje(),
        estudiante.getId().equals(yoId));
  }

  private Avance avanceConjunto(Desafio desafio, Instant ahora) {
    return avance(desafio, desafio.getCreadorId(), ahora)
        .mas(avance(desafio, desafio.getCompaneroId(), ahora));
  }

  private Avance avance(Desafio desafio, Long estudianteId, Instant ahora) {
    Instant hasta =
        (desafio.getCompletado() != null ? desafio.getCompletado() : ahora).plus(MARGEN);
    return actividadRepository.avanceEntre(estudianteId, desafio.getIniciado(), hasta);
  }

  private Estudiante buscar(Long estudianteId) {
    return estudianteRepository
        .findById(estudianteId)
        .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));
  }
}
