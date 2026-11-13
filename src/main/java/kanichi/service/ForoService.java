package kanichi.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.ForoSalon;
import kanichi.dto.MensajeForo;
import kanichi.model.Estudiante;
import kanichi.model.PublicacionForo;
import kanichi.model.RetoDocente;
import kanichi.model.TipoPublicacion;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.PublicacionForoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForoService {
  public static final ZoneId ZONA = ZoneId.of("America/Lima");
  public static final int LARGO_MENSAJE = 500;
  public static final int DIAS_RACHA = 7;
  public static final int MINIMO_EN_RACHA = 2;
  public static final String AUTOR_AUTOMATICO = "Kaichaini";

  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("dd/MM HH:mm");

  private final PublicacionForoRepository publicaciones;
  private final EstudianteRepository estudiantes;
  private final Clock reloj;

  @Autowired
  public ForoService(PublicacionForoRepository publicaciones, EstudianteRepository estudiantes) {
    this(publicaciones, estudiantes, Clock.systemUTC());
  }

  public ForoService(
      PublicacionForoRepository publicaciones, EstudianteRepository estudiantes, Clock reloj) {
    this.publicaciones = publicaciones;
    this.estudiantes = estudiantes;
    this.reloj = reloj;
  }

  @Transactional
  public ForoSalon verForo(Long estudianteId) {
    Estudiante estudiante = buscar(estudianteId);
    int grado = estudiante.getGrado();
    String seccion = estudiante.getSeccion();
    anunciarRachas(grado, seccion);

    List<PublicacionForo> recientes =
        publicaciones.findTop50ByGradoAndSeccionOrderByCreadaDescIdDesc(grado, seccion);
    Map<Long, Estudiante> autores =
        estudiantes
            .findAllById(
                recientes.stream()
                    .map(PublicacionForo::getAutorId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(Estudiante::getId, Function.identity()));

    List<MensajeForo> mensajes =
        recientes.stream().map(p -> aMensaje(p, autores.get(p.getAutorId()), estudianteId)).toList();
    return new ForoSalon(grado, seccion, mensajes);
  }

  @Transactional
  public void publicar(Long estudianteId, String texto) {
    String limpio = texto == null ? "" : texto.strip();
    if (limpio.isEmpty()) {
      throw new IllegalArgumentException("Escribe un mensaje antes de publicar.");
    }
    if (limpio.length() > LARGO_MENSAJE) {
      throw new IllegalArgumentException(
          "El mensaje no puede tener más de " + LARGO_MENSAJE + " caracteres.");
    }
    publicaciones.save(PublicacionForo.mensaje(buscar(estudianteId), limpio, reloj.instant()));
  }

  @Transactional
  public boolean anunciarRetoCompletado(RetoDocente reto, int grado, String seccion) {
    String texto =
        "🎉 ¡El salón "
            + grado
            + ".° "
            + seccion
            + " completó el reto «"
            + reto.getTitulo()
            + "»! Todos sus estudiantes lo terminaron.";
    return publicaciones.publicarAutomatica(
            grado,
            seccion,
            TipoPublicacion.RETO_SALON.name(),
            texto,
            "RETO:" + reto.getId() + ":" + seccion,
            reloj.instant())
        > 0;
  }

  @Transactional
  public boolean anunciarRachas(int grado, String seccion) {
    Instant ahora = reloj.instant();
    List<String> enRacha =
        estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, seccion).stream()
            .filter(e -> e.rachaEn(ahora).dias() >= DIAS_RACHA)
            .map(Estudiante::getNombre)
            .toList();
    if (enRacha.size() < MINIMO_EN_RACHA) {
      return false;
    }
    LocalDate lunes =
        LocalDate.ofInstant(ahora, ZONA).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    String texto =
        "🔥 ¡"
            + unirNombres(enRacha)
            + " llevan "
            + DIAS_RACHA
            + " días o más de racha! Sigan así.";
    return publicaciones.publicarAutomatica(
            grado,
            seccion,
            TipoPublicacion.RACHA.name(),
            texto,
            "RACHA:" + grado + ":" + seccion + ":" + lunes,
            ahora)
        > 0;
  }

  static String unirNombres(List<String> nombres) {
    if (nombres.size() == 1) {
      return nombres.get(0);
    }
    return String.join(", ", nombres.subList(0, nombres.size() - 1))
        + " y "
        + nombres.get(nombres.size() - 1);
  }

  private MensajeForo aMensaje(PublicacionForo publicacion, Estudiante autor, Long lectorId) {
    String fecha = FORMATO_FECHA.format(publicacion.getCreada().atZone(ZONA));
    if (publicacion.getTipo().isAutomatica()) {
      return new MensajeForo(
          AUTOR_AUTOMATICO, "🤖", publicacion.getTexto(), fecha, publicacion.getTipo(), false);
    }
    return new MensajeForo(
        autor == null ? "Estudiante" : autor.getNombre(),
        autor == null ? "👤" : autor.getAvatar().getIcono(),
        publicacion.getTexto(),
        fecha,
        publicacion.getTipo(),
        lectorId.equals(publicacion.getAutorId()));
  }

  private Estudiante buscar(Long estudianteId) {
    return estudiantes
        .findById(estudianteId)
        .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));
  }
}