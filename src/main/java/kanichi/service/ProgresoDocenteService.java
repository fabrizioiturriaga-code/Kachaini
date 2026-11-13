package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kanichi.dto.DetalleProgresoAlumno;
import kanichi.dto.DetalleProgresoAlumno.AvanceDiario;
import kanichi.dto.ProgresoAlumno;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.MultiplicadorXp;
import kanichi.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgresoDocenteService {
  public static final List<Integer> GRADOS = List.of(1, 2, 3, 4, 5);
  public static final List<String> SECCIONES = List.of("A", "B", "C", "D", "E", "F");
  public static final int DIAS_DEL_GRAFICO = 7;

  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private static final DateTimeFormatter FORMATO_DIA =
      DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("es-PE"));
  private static final DateTimeFormatter FORMATO_FECHA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final int LECCIONES_DEL_GRADO =
      Leccion.UNIDADES_POR_GRADO * Leccion.LECCIONES_POR_UNIDAD;

  private final EstudianteRepository estudiantes;
  private final MapaMisionesService mapa;
  private final MultiplicadorXpService multiplicadores;
  private final Clock reloj;

  @Autowired
  public ProgresoDocenteService(
      EstudianteRepository estudiantes,
      MapaMisionesService mapa,
      MultiplicadorXpService multiplicadores) {
    this(estudiantes, mapa, multiplicadores, Clock.systemUTC());
  }

  public ProgresoDocenteService(
      EstudianteRepository estudiantes,
      MapaMisionesService mapa,
      MultiplicadorXpService multiplicadores,
      Clock reloj) {
    this.estudiantes = estudiantes;
    this.mapa = mapa;
    this.multiplicadores = multiplicadores;
    this.reloj = reloj;
  }

  public static boolean esGradoValido(Integer grado) {
    return grado != null && GRADOS.contains(grado);
  }

  public static String normalizarSeccion(String seccion) {
    if (seccion == null || seccion.isBlank()) {
      return null;
    }
    String valor = seccion.strip().toUpperCase(Locale.ROOT);
    return SECCIONES.contains(valor) ? valor : null;
  }

  @Transactional(readOnly = true)
  public List<ProgresoAlumno> listar(int grado, String seccion) {
    if (!esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    String seccionValida = normalizarSeccion(seccion);
    List<Estudiante> salon = new ArrayList<>();
    for (String s : seccionValida == null ? SECCIONES : List.of(seccionValida)) {
      salon.addAll(estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, s));
    }
    Map<Long, MultiplicadorXp> activos =
        multiplicadores.activos(salon.stream().map(Estudiante::getId).toList());
    Instant ahora = reloj.instant();
    return salon.stream().map(e -> resumen(e, activos.get(e.getId()), ahora)).toList();
  }

  @Transactional(readOnly = true)
  public DetalleProgresoAlumno detalle(Long estudianteId) {
    Estudiante estudiante =
        estudiantes
            .findById(estudianteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Instant ahora = reloj.instant();
    ProgresoAlumno resumen =
        resumen(
            estudiante, multiplicadores.activos(List.of(estudianteId)).get(estudianteId), ahora);
    return new DetalleProgresoAlumno(
        resumen,
        estudiante.getGrado(),
        mapa.obtenerMapa(
            estudiante.getGrado(),
            estudiante.isOnboardingCompletado(),
            estudiante.getLeccionesCompletadas()),
        avanceSemanal(estudianteId, ahora));
  }

  private ProgresoAlumno resumen(Estudiante estudiante, MultiplicadorXp activo, Instant ahora) {
    long completadasDelGrado =
        estudiante.getLeccionesCompletadas().stream()
            .filter(l -> l.getGrado() == estudiante.getGrado())
            .count();
    return new ProgresoAlumno(
        estudiante.getId(),
        estudiante.getNombre(),
        estudiante.getAvatar().getIcono(),
        estudiante.getSeccion(),
        (int) (completadasDelGrado * 100 / LECCIONES_DEL_GRADO),
        estudiante.cantidadMisionesCompletadas(),
        estudiante.getLeccionesCompletadas().size(),
        estudiante.getXp(),
        estudiante.rachaEn(ahora).dias(),
        activo == null ? null : activo.getFactor().getEtiqueta(),
        activo == null ? null : FORMATO_FECHA.format(activo.getFin().atZone(ZONA)));
  }

  private List<AvanceDiario> avanceSemanal(Long estudianteId, Instant ahora) {
    LocalDate hoy = LocalDate.ofInstant(ahora, ZONA);
    List<LocalDate> dias = new ArrayList<>();
    List<Integer> xp = new ArrayList<>();
    for (int i = DIAS_DEL_GRAFICO - 1; i >= 0; i--) {
      LocalDate dia = hoy.minusDays(i);
      dias.add(dia);
      xp.add(
          estudiantes.sumarXpEntre(
              estudianteId,
              dia.atStartOfDay(ZONA).toInstant(),
              dia.plusDays(1).atStartOfDay(ZONA).toInstant()));
    }
    int maximo = xp.stream().mapToInt(Integer::intValue).max().orElse(0);
    List<AvanceDiario> semana = new ArrayList<>();
    for (int i = 0; i < dias.size(); i++) {
      int valor = xp.get(i);
      semana.add(
          new AvanceDiario(
              FORMATO_DIA.format(dias.get(i)), valor, maximo == 0 ? 0 : valor * 100 / maximo));
    }
    return semana;
  }
}
