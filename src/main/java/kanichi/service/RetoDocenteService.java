package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.AvisoReto;
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
  private final Clock reloj;

  @Autowired
  public RetoDocenteService(
      RetoDocenteRepository retos,
      NotificacionRepository notificaciones,
      EstudianteRepository estudiantes) {
    this(retos, notificaciones, estudiantes, Clock.systemUTC());
  }

  public RetoDocenteService(
      RetoDocenteRepository retos,
      NotificacionRepository notificaciones,
      EstudianteRepository estudiantes,
      Clock reloj) {
    this.retos = retos;
    this.notificaciones = notificaciones;
    this.estudiantes = estudiantes;
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
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
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

    List<Estudiante> salon = new ArrayList<>();
    for (String s :
        seccionValida == null ? ProgresoDocenteService.SECCIONES : List.of(seccionValida)) {
      salon.addAll(estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, s));
    }
    if (salon.isEmpty()) {
      throw new IllegalArgumentException("No hay estudiantes registrados en ese salón.");
    }

    Instant ahora = reloj.instant();
    RetoDocente reto =
        retos.save(
            new RetoDocente(
                docenteId,
                grado,
                seccionValida,
                tituloLimpio,
                descripcionLimpia,
                objetivosLimpios,
                fechaLimite,
                xp,
                ahora));
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
                    notificaciones.countByRetoIdAndLeidaIsNotNull(reto.getId())))
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
              return new AvisoReto(reto, n.isLeida(), reto.estadoEn(hoy));
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

  
  public AvisoReto abrir(Long estudianteId, Long retoId) {
    Notificacion notificacion =
        notificaciones
            .findByEstudianteIdAndRetoId(estudianteId, retoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    RetoDocente reto =
        retos
            .findById(retoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    notificacion.marcarLeida(reloj.instant());
    return new AvisoReto(reto, true, reto.estadoEn(hoy()));
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