package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.AvisoReto;
import kanichi.dto.ResultadoReto;
import kanichi.dto.ResumenAvisos;
import kanichi.dto.RetoPublicado;
import kanichi.model.Estudiante;
import kanichi.model.Notificacion;
import kanichi.model.RetoDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.NotificacionRepository;
import kanichi.repository.RetoDocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RetoDocenteService {
  public static final ZoneId ZONA = ZoneId.of("America/Lima");
  public static final int XP_MAXIMA = 100;
  public static final int DIAS_MAXIMOS = 90;
  public static final int LARGO_TITULO = 100;
  public static final int LARGO_TEXTO = 1000;

  private final RetoDocenteRepository retos;
  private final NotificacionRepository notificaciones;
  private final EstudianteRepository estudiantes;
  private final ForoService foroService;
  private final Clock reloj;

  @Autowired
  public RetoDocenteService(
      RetoDocenteRepository retos,
      NotificacionRepository notificaciones,
      EstudianteRepository estudiantes,
      ForoService foroService) {
    this(retos, notificaciones, estudiantes, foroService, Clock.systemUTC());
  }

  public RetoDocenteService(
      RetoDocenteRepository retos,
      NotificacionRepository notificaciones,
      EstudianteRepository estudiantes,
      ForoService foroService,
      Clock reloj) {
    this.retos = retos;
    this.notificaciones = notificaciones;
    this.estudiantes = estudiantes;
    this.foroService = foroService;
    this.reloj = reloj;
  }

  @Transactional
  public int publicar(
      Long docenteId,
      Integer grado,
      String seccion,
      String titulo,
      String descripcion,
      String objetivos,
      LocalDate fechaLimite,
      Integer xp) {
    return publicar(
        docenteId, grado, seccion, titulo, descripcion, objetivos, fechaLimite, xp, null, null, null);
  }

  @Transactional
  public int publicar(
      Long docenteId,
      Integer grado,
      String seccion,
      String titulo,
      String descripcion,
      String objetivos,
      LocalDate fechaLimite,
      Integer xp,
      String pregunta,
      List<String> opciones,
      Integer respuestaCorrecta) {
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    if (seccion != null && !seccion.isBlank() && seccionValida == null) {
      throw new IllegalArgumentException("Selecciona la sección A, B o C.");
    }
    String tituloLimpio = obligatorio(titulo, LARGO_TITULO, "El título");
    String descripcionLimpia = obligatorio(descripcion, LARGO_TEXTO, "La descripción");
    String objetivosLimpios = obligatorio(objetivos, LARGO_TEXTO, "Los objetivos");
    LocalDate hoy = hoy();
    if (fechaLimite == null || fechaLimite.isBefore(hoy)) {
      throw new IllegalArgumentException("La fecha límite debe ser hoy o una fecha futura.");
    }
    if (fechaLimite.isAfter(hoy.plusDays(DIAS_MAXIMOS))) {
      throw new IllegalArgumentException(
          "La fecha límite no puede ser mayor a " + DIAS_MAXIMOS + " días.");
    }
    if (xp == null || xp < 0 || xp > XP_MAXIMA) {
      throw new IllegalArgumentException("La recompensa debe ser de 0 a " + XP_MAXIMA + " XP.");
    }
    PreguntaValidada preguntaValidada = validarPregunta(pregunta, opciones, respuestaCorrecta);

    List<Estudiante> salon = new ArrayList<>();
    for (String s :
        seccionValida == null ? ProgresoDocenteService.SECCIONES : List.of(seccionValida)) {
      salon.addAll(estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, s));
    }
    if (salon.isEmpty()) {
      throw new IllegalArgumentException("No hay estudiantes registrados en ese salón.");
    }

    Instant ahora = reloj.instant();
    RetoDocente nuevo =
        new RetoDocente(
            docenteId,
            grado,
            seccionValida,
            tituloLimpio,
            descripcionLimpia,
            objetivosLimpios,
            fechaLimite,
            xp,
            ahora);
    if (preguntaValidada != null) {
      nuevo.agregarPregunta(
          preguntaValidada.texto(), preguntaValidada.opciones(), preguntaValidada.correcta());
    }
    RetoDocente reto = retos.save(nuevo);
    notificaciones.saveAll(
        salon.stream().map(e -> new Notificacion(e.getId(), reto.getId(), ahora)).toList());
    return salon.size();
  }

  @Transactional(readOnly = true)
  public List<RetoPublicado> publicadosPor(Long docenteId) {
    return retos.findByDocenteIdOrderByPublicadoDesc(docenteId).stream()
        .map(
            reto ->
                new RetoPublicado(
                    reto,
                    notificaciones.countByRetoId(reto.getId()),
                    notificaciones.countByRetoIdAndLeidaIsNotNull(reto.getId()),
                    nombresDeQuienesCompletaron(reto.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AvisoReto> avisos(Long estudianteId) {
    List<Notificacion> recibidas = notificaciones.findByEstudianteIdOrderByCreadaDesc(estudianteId);
    Map<Long, RetoDocente> porId =
        retos.findAllById(recibidas.stream().map(Notificacion::getRetoId).toList()).stream()
            .collect(Collectors.toMap(RetoDocente::getId, Function.identity()));
    LocalDate hoy = hoy();
    return recibidas.stream()
        .filter(n -> porId.containsKey(n.getRetoId()))
        .map(
            n -> {
              RetoDocente reto = porId.get(n.getRetoId());
              return new AvisoReto(
                  reto, n.isLeida(), n.isCompletada(), reto.estadoEn(hoy), n.isFallida());
            })
        .sorted(Comparator.comparing(AvisoReto::leida))
        .toList();
  }

  @Transactional(readOnly = true)
  public ResumenAvisos resumen(Long estudianteId) {
    List<AvisoReto> avisos = avisos(estudianteId);
    int nuevos = (int) avisos.stream().filter(a -> !a.leida()).count();
    int porVencer = (int) avisos.stream().filter(AvisoReto::isPorVencer).count();
    return new ResumenAvisos(nuevos, porVencer);
  }

  @Transactional
  public AvisoReto abrir(Long estudianteId, Long retoId) {
    Notificacion notificacion = notificacionDe(estudianteId, retoId);
    RetoDocente reto = retoPorId(retoId);
    notificacion.marcarLeida(reloj.instant());
    return new AvisoReto(
        reto, true, notificacion.isCompletada(), reto.estadoEn(hoy()), notificacion.isFallida());
  }

  @Transactional
  public ResultadoReto completar(Long estudianteId, Long retoId) {
    return completar(estudianteId, retoId, null);
  }

  @Transactional
  public ResultadoReto completar(Long estudianteId, Long retoId, Integer respuesta) {
    Estudiante actual =
        estudiantes
            .findById(estudianteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    int grado = actual.getGrado();
    String seccion = actual.getSeccion();
    Estudiante estudiante =
        estudiantes.bloquearSalon(grado, seccion).stream()
            .filter(e -> estudianteId.equals(e.getId()))
            .findFirst()
            .orElse(actual);

    Notificacion notificacion = notificacionDe(estudianteId, retoId);
    RetoDocente reto = retoPorId(retoId);
    if (notificacion.isCompletada()) {
      throw new IllegalArgumentException("Ya completaste este reto.");
    }
    if (notificacion.isFallida()) {
      throw new IllegalArgumentException("Ya respondiste este reto y tu respuesta no fue correcta.");
    }
    if (reto.estadoEn(hoy()) == RetoDocente.Estado.VENCIDO) {
      throw new IllegalArgumentException("Este reto ya venció.");
    }

    Instant ahora = reloj.instant();
    if (reto.isConPregunta()) {
      if (respuesta == null) {
        throw new IllegalArgumentException("Elige una respuesta antes de enviar.");
      }
      if (!reto.esCorrecta(respuesta)) {
        notificacion.registrarFallo(ahora);
        return ResultadoReto.incorrecto();
      }
    }

    notificacion.completar(ahora);
    int xp = reto.getXp();
    if (xp > 0) {
      estudiante.sumarXp(xp);
      estudiantes.registrarXp(estudianteId, xp, ahora);
    }

    boolean salonCompleto =
        notificaciones.contarPendientesDelSalon(retoId, grado, seccion) == 0
            && foroService.anunciarRetoCompletado(reto, grado, seccion);
    return new ResultadoReto(xp, salonCompleto);
  }

  private record PreguntaValidada(String texto, List<String> opciones, int correcta) {}

  private static PreguntaValidada validarPregunta(
      String pregunta, List<String> opciones, Integer respuestaCorrecta) {
    String texto = pregunta == null ? "" : pregunta.strip();
    List<String> campos = opciones == null ? List.of() : opciones;
    if (texto.isEmpty()) {
      boolean hayOpciones = campos.stream().anyMatch(o -> o != null && !o.isBlank());
      if (hayOpciones) {
        throw new IllegalArgumentException("Escribe la pregunta o deja vacías sus opciones.");
      }
      return null;
    }
    if (texto.length() > RetoDocente.LARGO_PREGUNTA) {
      throw new IllegalArgumentException(
          "La pregunta no puede tener más de " + RetoDocente.LARGO_PREGUNTA + " caracteres.");
    }
    if (campos.size() > RetoDocente.MAXIMO_OPCIONES) {
      throw new IllegalArgumentException(
          "Puedes poner como máximo " + RetoDocente.MAXIMO_OPCIONES + " opciones.");
    }
    List<String> llenas = new ArrayList<>();
    int correcta = -1;
    for (int i = 0; i < campos.size(); i++) {
      String opcion = campos.get(i) == null ? "" : campos.get(i).strip();
      if (opcion.isEmpty()) {
        continue;
      }
      if (opcion.length() > RetoDocente.LARGO_OPCION) {
        throw new IllegalArgumentException(
            "Cada opción puede tener como máximo " + RetoDocente.LARGO_OPCION + " caracteres.");
      }
      if (respuestaCorrecta != null && respuestaCorrecta == i) {
        correcta = llenas.size();
      }
      llenas.add(opcion);
    }
    if (llenas.size() < 2) {
      throw new IllegalArgumentException("La pregunta necesita al menos 2 opciones.");
    }
    long distintas = llenas.stream().map(o -> o.toLowerCase(Locale.ROOT)).distinct().count();
    if (distintas < llenas.size()) {
      throw new IllegalArgumentException("Las opciones deben ser distintas.");
    }
    if (correcta < 0) {
      throw new IllegalArgumentException("Marca cuál de las opciones es la correcta.");
    }
    return new PreguntaValidada(texto, List.copyOf(llenas), correcta);
  }

  private List<String> nombresDeQuienesCompletaron(Long retoId) {
    List<Long> ids =
        notificaciones.findByRetoIdAndCompletadaIsNotNullOrderByCompletadaAsc(retoId).stream()
            .map(Notificacion::getEstudianteId)
            .toList();
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<Long, String> nombres =
        estudiantes.findAllById(ids).stream()
            .collect(Collectors.toMap(Estudiante::getId, Estudiante::getNombre));
    return ids.stream().filter(nombres::containsKey).map(nombres::get).toList();
  }

  private Notificacion notificacionDe(Long estudianteId, Long retoId) {
    return notificaciones
        .findByEstudianteIdAndRetoId(estudianteId, retoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private RetoDocente retoPorId(Long retoId) {
    return retos
        .findById(retoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private LocalDate hoy() {
    return LocalDate.now(reloj.withZone(ZONA));
  }

  private static String obligatorio(String valor, int largoMaximo, String campo) {
    String limpio = valor == null ? "" : valor.strip();
    if (limpio.isEmpty()) {
      throw new IllegalArgumentException(campo + " es obligatorio.");
    }
    if (limpio.length() > largoMaximo) {
      throw new IllegalArgumentException(
          campo + " no puede tener más de " + largoMaximo + " caracteres.");
    }
    return limpio;
  }
}