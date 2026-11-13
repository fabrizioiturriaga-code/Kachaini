package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.Conversacion;
import kanichi.dto.Conversacion.Contacto;
import kanichi.dto.Conversacion.MensajeChat;
import kanichi.dto.Conversacion.Resumen;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.MensajeDirecto;
import kanichi.repository.DocenteRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.MensajeDirectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MensajeriaService {
  public static final int LARGO_MENSAJE = 1000;
  public static final int LARGO_VISTA_PREVIA = 80;
  public static final String AVATAR_DOCENTE = "🧑‍🏫";

  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("dd/MM HH:mm");

  private final MensajeDirectoRepository mensajes;
  private final EstudianteRepository estudiantes;
  private final DocenteRepository docentes;
  private final Clock reloj;

  @Autowired
  public MensajeriaService(
      MensajeDirectoRepository mensajes,
      EstudianteRepository estudiantes,
      DocenteRepository docentes) {
    this(mensajes, estudiantes, docentes, Clock.systemUTC());
  }

  public MensajeriaService(
      MensajeDirectoRepository mensajes,
      EstudianteRepository estudiantes,
      DocenteRepository docentes,
      Clock reloj) {
    this.mensajes = mensajes;
    this.estudiantes = estudiantes;
    this.docentes = docentes;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public List<Resumen> bandejaDocente(Long docenteId) {
    Map<Long, List<MensajeDirecto>> porEstudiante =
        agrupar(
            mensajes.findByDocenteIdOrderByEnviadoDescIdDesc(docenteId),
            MensajeDirecto::getEstudianteId);
    Map<Long, Estudiante> contactos =
        estudiantes.findAllById(porEstudiante.keySet()).stream()
            .collect(Collectors.toMap(Estudiante::getId, Function.identity()));
    List<Resumen> bandeja = new ArrayList<>();
    porEstudiante.forEach(
        (estudianteId, conversacion) -> {
          Estudiante estudiante = contactos.get(estudianteId);
          if (estudiante != null) {
            bandeja.add(resumen(contacto(estudiante), conversacion, true));
          }
        });
    return bandeja;
  }

  @Transactional(readOnly = true)
  public List<Resumen> bandejaEstudiante(Long estudianteId) {
    Map<Long, List<MensajeDirecto>> porDocente =
        agrupar(
            mensajes.findByEstudianteIdOrderByEnviadoDescIdDesc(estudianteId),
            MensajeDirecto::getDocenteId);
    Map<Long, Docente> contactos =
        docentes.findAllById(porDocente.keySet()).stream()
            .collect(Collectors.toMap(Docente::getId, Function.identity()));
    List<Resumen> bandeja = new ArrayList<>();
    porDocente.forEach(
        (docenteId, conversacion) -> {
          Docente docente = contactos.get(docenteId);
          if (docente != null) {
            bandeja.add(resumen(contacto(docente), conversacion, false));
          }
        });
    return bandeja;
  }

  @Transactional(readOnly = true)
  public List<Contacto> estudiantesDelSalon(int grado, String seccion) {
    return salon(grado, seccion).stream().map(MensajeriaService::contacto).toList();
  }

  @Transactional(readOnly = true)
  public List<Contacto> docentesDisponibles() {
    return docentes.findByAutorizadoTrueOrderByNombreAsc().stream()
        .map(MensajeriaService::contacto)
        .toList();
  }

  @Transactional
  public int enviarAEstudiantes(
      Long docenteId,
      Integer grado,
      String seccion,
      List<Long> seleccionados,
      boolean todoElSalon,
      String texto) {
    String limpio = validarTexto(texto);
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    List<Estudiante> salon = salon(grado, seccion);
    List<Estudiante> destinatarios;
    if (todoElSalon) {
      if (salon.isEmpty()) {
        throw new IllegalArgumentException("No hay estudiantes registrados en este salón.");
      }
      destinatarios = salon;
    } else {
      if (seleccionados == null || seleccionados.isEmpty()) {
        throw new IllegalArgumentException(
            "Elige al menos un estudiante o envía el mensaje a todo el salón.");
      }
      Set<Long> elegidos = Set.copyOf(seleccionados);
      destinatarios = salon.stream().filter(e -> elegidos.contains(e.getId())).toList();
      if (destinatarios.size() != elegidos.size()) {
        throw new IllegalArgumentException(
            "Solo puedes elegir estudiantes del grado seleccionado.");
      }
    }
    Instant ahora = reloj.instant();
    mensajes.saveAll(
        destinatarios.stream()
            .map(e -> MensajeDirecto.deDocente(docenteId, e.getId(), limpio, ahora))
            .toList());
    return destinatarios.size();
  }

  @Transactional
  public Conversacion conversacionDocente(Long docenteId, Long estudianteId) {
    Estudiante estudiante = buscarEstudiante(estudianteId);
    mensajes.marcarLeidos(docenteId, estudianteId, false, reloj.instant());
    return new Conversacion(
        contacto(estudiante), chat(docenteId, estudianteId, true));
  }

  @Transactional
  public void responderComoDocente(Long docenteId, Long estudianteId, String texto) {
    String limpio = validarTexto(texto);
    buscarEstudiante(estudianteId);
    mensajes.save(MensajeDirecto.deDocente(docenteId, estudianteId, limpio, reloj.instant()));
  }

  @Transactional
  public Conversacion conversacionEstudiante(Long estudianteId, Long docenteId) {
    Docente docente =
        docentes
            .findById(docenteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    mensajes.marcarLeidos(docenteId, estudianteId, true, reloj.instant());
    return new Conversacion(contacto(docente), chat(docenteId, estudianteId, false));
  }

  @Transactional
  public void enviarComoEstudiante(Long estudianteId, Long docenteId, String texto) {
    if (docenteId == null) {
      throw new IllegalArgumentException("Elige un docente.");
    }
    String limpio = validarTexto(texto);
    Docente docente =
        docentes
            .findById(docenteId)
            .filter(Docente::isAutorizado)
            .orElseThrow(
                () -> new IllegalArgumentException("El docente elegido no está disponible."));
    mensajes.save(
        MensajeDirecto.deEstudiante(estudianteId, docente.getId(), limpio, reloj.instant()));
  }

  @Transactional(readOnly = true)
  public int sinLeerDocente(Long docenteId) {
    return (int) mensajes.countByDocenteIdAndEnviadoPorDocenteFalseAndLeidoIsNull(docenteId);
  }

  @Transactional(readOnly = true)
  public int sinLeerEstudiante(Long estudianteId) {
    return (int) mensajes.countByEstudianteIdAndEnviadoPorDocenteTrueAndLeidoIsNull(estudianteId);
  }

  static String vistaPrevia(String texto) {
    String linea = texto.replaceAll("\\s+", " ").strip();
    return linea.length() <= LARGO_VISTA_PREVIA
        ? linea
        : linea.substring(0, LARGO_VISTA_PREVIA - 1).stripTrailing() + "…";
  }

  private static String validarTexto(String texto) {
    String limpio = texto == null ? "" : texto.strip();
    if (limpio.isEmpty()) {
      throw new IllegalArgumentException("Escribe un mensaje antes de enviarlo.");
    }
    if (limpio.length() > LARGO_MENSAJE) {
      throw new IllegalArgumentException(
          "El mensaje no puede tener más de " + LARGO_MENSAJE + " caracteres.");
    }
    return limpio;
  }

  private List<Estudiante> salon(int grado, String seccion) {
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    List<Estudiante> salon = new ArrayList<>();
    for (String s :
        seccionValida == null ? ProgresoDocenteService.SECCIONES : List.of(seccionValida)) {
      salon.addAll(estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, s));
    }
    return salon;
  }

  private List<MensajeChat> chat(Long docenteId, Long estudianteId, boolean lectorEsDocente) {
    return mensajes
        .findByDocenteIdAndEstudianteIdOrderByEnviadoAscIdAsc(docenteId, estudianteId)
        .stream()
        .map(
            m ->
                new MensajeChat(
                    m.getTexto(),
                    formatear(m.getEnviado()),
                    m.isEnviadoPorDocente() == lectorEsDocente,
                    m.isLeido()))
        .toList();
  }

  private static Resumen resumen(
      Contacto contacto, List<MensajeDirecto> conversacion, boolean lectorEsDocente) {
    MensajeDirecto ultimo = conversacion.get(0);
    int sinLeer =
        (int)
            conversacion.stream()
                .filter(m -> m.isEnviadoPorDocente() != lectorEsDocente && !m.isLeido())
                .count();
    return new Resumen(
        contacto,
        vistaPrevia(ultimo.getTexto()),
        formatear(ultimo.getEnviado()),
        ultimo.isEnviadoPorDocente() == lectorEsDocente,
        sinLeer);
  }

  private static <K> Map<K, List<MensajeDirecto>> agrupar(
      List<MensajeDirecto> recientes, Function<MensajeDirecto, K> clave) {
    Map<K, List<MensajeDirecto>> grupos = new LinkedHashMap<>();
    for (MensajeDirecto mensaje : recientes) {
      grupos.computeIfAbsent(clave.apply(mensaje), k -> new ArrayList<>()).add(mensaje);
    }
    return grupos;
  }

  private Estudiante buscarEstudiante(Long estudianteId) {
    return estudiantes
        .findById(estudianteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static Contacto contacto(Estudiante estudiante) {
    return new Contacto(
        estudiante.getId(),
        estudiante.getNombre(),
        estudiante.getAvatar().getIcono(),
        estudiante.getGrado() + ".° " + estudiante.getSeccion());
  }

  private static Contacto contacto(Docente docente) {
    return new Contacto(docente.getId(), docente.getNombre(), AVATAR_DOCENTE, docente.getArea());
  }

  private static String formatear(Instant instante) {
    return FORMATO_FECHA.format(instante.atZone(ZONA));
  }
}
