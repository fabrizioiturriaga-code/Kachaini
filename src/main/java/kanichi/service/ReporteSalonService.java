package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.dto.DetalleProgresoAlumno.AvanceDiario;
import kanichi.dto.ReporteSalon;
import kanichi.dto.ReporteSalon.FilaReporte;
import kanichi.dto.ReporteSalon.UnidadReporte;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.ReporteSalonRepository;
import kanichi.repository.ReporteSalonRepository.IntentosPorEstudiante;
import kanichi.repository.ReporteSalonRepository.XpPorEstudiante;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteSalonService {
  public static final int DIAS_DEL_REPORTE = 7;
  public static final int MINIMO_INTENTOS = 5;
  public static final int ACIERTOS_MINIMOS = 60;
  public static final int CANTIDAD_DESTACADOS = 3;

  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private static final DateTimeFormatter FORMATO_DIA =
      DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("es-PE"));
  private static final DateTimeFormatter FORMATO_GENERADO =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final int LECCIONES_DEL_GRADO =
      Leccion.UNIDADES_POR_GRADO * Leccion.LECCIONES_POR_UNIDAD;
  private static final String SEPARADOR = ";";

  private final EstudianteRepository estudiantes;
  private final ReporteSalonRepository reportes;
  private final MapaMisionesService mapa;
  private final Clock reloj;

  @Autowired
  public ReporteSalonService(
      EstudianteRepository estudiantes,
      ReporteSalonRepository reportes,
      MapaMisionesService mapa) {
    this(estudiantes, reportes, mapa, Clock.systemUTC());
  }

  public ReporteSalonService(
      EstudianteRepository estudiantes,
      ReporteSalonRepository reportes,
      MapaMisionesService mapa,
      Clock reloj) {
    this.estudiantes = estudiantes;
    this.reportes = reportes;
    this.mapa = mapa;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public ReporteSalon generar(int grado, String seccion) {
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      throw new IllegalArgumentException("Selecciona un grado de 1 a 5.");
    }
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    List<Estudiante> salon = new ArrayList<>();
    for (String s :
        seccionValida == null ? ProgresoDocenteService.SECCIONES : List.of(seccionValida)) {
      salon.addAll(estudiantes.findByGradoAndSeccionOrderByNombreAsc(grado, s));
    }

    Instant ahora = reloj.instant();
    LocalDate hoy = LocalDate.ofInstant(ahora, ZONA);
    Instant inicioSemana = inicioDelDia(hoy.minusDays(DIAS_DEL_REPORTE - 1));
    Instant finSemana = inicioDelDia(hoy.plusDays(1));
    Instant inicioAnterior = inicioDelDia(hoy.minusDays(2L * DIAS_DEL_REPORTE - 1));
    List<Long> ids = salon.stream().map(Estudiante::getId).toList();

    Map<Long, Integer> xpSemanal = Map.of();
    Map<Long, IntentosPorEstudiante> intentos = Map.of();
    int xpSemanaAnterior = 0;
    if (!ids.isEmpty()) {
      xpSemanal =
          reportes.xpPorEstudianteEntre(ids, inicioSemana, finSemana).stream()
              .collect(Collectors.toMap(XpPorEstudiante::getId, XpPorEstudiante::getPuntos));
      intentos =
          reportes.intentosPorEstudiante(ids).stream()
              .collect(Collectors.toMap(IntentosPorEstudiante::getId, Function.identity()));
      xpSemanaAnterior = reportes.sumarXpEntre(ids, inicioAnterior, inicioSemana);
    }

    List<FilaReporte> filas = new ArrayList<>();
    for (Estudiante estudiante : salon) {
      filas.add(
          fila(
              estudiante,
              xpSemanal.getOrDefault(estudiante.getId(), 0),
              intentos.get(estudiante.getId()),
              ahora));
    }

    List<UnidadReporte> unidades =
        Leccion.unidadesDelGrado(grado).stream().map(u -> unidad(u, salon)).toList();
    List<FilaReporte> destacados =
        filas.stream()
            .filter(f -> f.xpSemana() > 0)
            .sorted(
                Comparator.comparingInt(FilaReporte::xpSemana)
                    .reversed()
                    .thenComparing(FilaReporte::nombre))
            .limit(CANTIDAD_DESTACADOS)
            .toList();
    List<FilaReporte> requierenApoyo = filas.stream().filter(FilaReporte::necesitaApoyo).toList();

    long aciertos = filas.stream().mapToLong(FilaReporte::aciertos).sum();
    long errores = filas.stream().mapToLong(FilaReporte::errores).sum();
    Integer porcentajeAciertos =
        aciertos + errores == 0 ? null : (int) (aciertos * 100 / (aciertos + errores));
    int xpSemana = filas.stream().mapToInt(FilaReporte::xpSemana).sum();
    int activos = (int) filas.stream().filter(f -> f.xpSemana() > 0).count();

    List<String> observaciones = new ArrayList<>();
    ReporteSalon reporte =
        new ReporteSalon(
            grado,
            seccionValida,
            FORMATO_GENERADO.format(ahora.atZone(ZONA)),
            promedio(filas.stream().mapToInt(FilaReporte::porcentajeAvance).sum(), filas.size()),
            filas.stream().mapToInt(FilaReporte::xp).sum(),
            xpSemana,
            xpSemanaAnterior,
            activos,
            promedio(filas.stream().mapToInt(FilaReporte::diasRacha).sum(), filas.size()),
            porcentajeAciertos,
            avanceDelSalon(ids, hoy),
            unidades,
            filas,
            destacados,
            requierenApoyo,
            observaciones);
    observaciones.addAll(observaciones(reporte));
    return reporte;
  }

  public String exportarCsv(ReporteSalon reporte) {
    StringBuilder csv = new StringBuilder("\uFEFF");
    linea(csv, "Reporte de rendimiento", reporte.salon());
    linea(csv, "Generado", reporte.generado());
    linea(csv, "Estudiantes", reporte.totalEstudiantes());
    linea(csv, "Avance promedio del grado (%)", reporte.promedioAvance());
    linea(csv, "XP total", reporte.xpTotal());
    linea(csv, "XP de los últimos 7 días", reporte.xpSemana());
    linea(csv, "XP de los 7 días anteriores", reporte.xpSemanaAnterior());
    linea(
        csv,
        "Estudiantes activos en los últimos 7 días",
        reporte.activosSemana() + " (" + reporte.porcentajeActivos() + " %)");
    linea(csv, "Racha promedio (días)", reporte.promedioRacha());
    linea(
        csv,
        "Aciertos del salón (%)",
        reporte.porcentajeAciertos() == null ? "Sin intentos" : reporte.porcentajeAciertos());
    csv.append("\r\n");

    linea(csv, "Unidad", "Título", "Completaron", "En curso", "Sin empezar", "Completaron (%)");
    for (UnidadReporte unidad : reporte.unidades()) {
      linea(
          csv,
          unidad.numero(),
          unidad.titulo(),
          unidad.completaron(),
          unidad.enCurso(),
          unidad.sinEmpezar(),
          unidad.porcentaje());
    }
    csv.append("\r\n");

    linea(
        csv,
        "Estudiante",
        "Sección",
        "Avance del grado (%)",
        "Lecciones completadas",
        "XP total",
        "XP últimos 7 días",
        "Racha (días)",
        "Aciertos",
        "Errores",
        "Aciertos (%)",
        "Observación");
    for (FilaReporte fila : reporte.filas()) {
      linea(
          csv,
          fila.nombre(),
          fila.seccion(),
          fila.porcentajeAvance(),
          fila.leccionesCompletadas(),
          fila.xp(),
          fila.xpSemana(),
          fila.diasRacha(),
          fila.aciertos(),
          fila.errores(),
          fila.porcentajeAciertos() == null ? "" : fila.porcentajeAciertos(),
          fila.motivoApoyo() == null ? "" : fila.motivoApoyo());
    }
    csv.append("\r\n");

    linea(csv, "Observaciones");
    for (String observacion : reporte.observaciones()) {
      linea(csv, observacion);
    }
    return csv.toString();
  }

  public String nombreArchivo(ReporteSalon reporte) {
    String salon = reporte.grado() + (reporte.seccion() == null ? "-todas" : reporte.seccion());
    return "reporte-"
        + salon
        + "-"
        + FORMATO_ARCHIVO.format(LocalDate.ofInstant(reloj.instant(), ZONA))
        + ".csv";
  }

  static String celda(Object valor) {
    String texto = valor == null ? "" : valor.toString();
    if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
      texto = "'" + texto;
    }
    if (texto.contains(SEPARADOR)
        || texto.contains("\"")
        || texto.contains("\n")
        || texto.contains("\r")) {
      texto = "\"" + texto.replace("\"", "\"\"") + "\"";
    }
    return texto;
  }

  private static void linea(StringBuilder csv, Object... valores) {
    for (int i = 0; i < valores.length; i++) {
      if (i > 0) {
        csv.append(SEPARADOR);
      }
      csv.append(celda(valores[i]));
    }
    csv.append("\r\n");
  }

  private FilaReporte fila(
      Estudiante estudiante, int xpSemana, IntentosPorEstudiante intentos, Instant ahora) {
    long completadasDelGrado =
        estudiante.getLeccionesCompletadas().stream()
            .filter(l -> l.getGrado() == estudiante.getGrado())
            .count();
    long aciertos = intentos == null ? 0 : intentos.getAciertos();
    long errores = intentos == null ? 0 : intentos.getErrores();
    return new FilaReporte(
        estudiante.getId(),
        estudiante.getNombre(),
        estudiante.getAvatar().getIcono(),
        estudiante.getSeccion(),
        (int) (completadasDelGrado * 100 / LECCIONES_DEL_GRADO),
        (int) completadasDelGrado,
        estudiante.getXp(),
        xpSemana,
        estudiante.rachaEn(ahora).dias(),
        aciertos,
        errores,
        motivoApoyo(xpSemana, aciertos, errores));
  }

  private static String motivoApoyo(int xpSemana, long aciertos, long errores) {
    List<String> motivos = new ArrayList<>();
    if (xpSemana == 0) {
      motivos.add("Sin actividad en los últimos 7 días");
    }
    long intentos = aciertos + errores;
    if (intentos >= MINIMO_INTENTOS && aciertos * 100 / intentos < ACIERTOS_MINIMOS) {
      motivos.add("Aciertos por debajo del " + ACIERTOS_MINIMOS + " %");
    }
    return motivos.isEmpty() ? null : String.join(". ", motivos) + ".";
  }

  private UnidadReporte unidad(int numero, List<Estudiante> salon) {
    List<Leccion> lecciones = Leccion.deUnidad(numero);
    int completaron = 0;
    int enCurso = 0;
    for (Estudiante estudiante : salon) {
      long hechas = lecciones.stream().filter(estudiante::haCompletado).count();
      if (hechas == lecciones.size()) {
        completaron++;
      } else if (hechas > 0) {
        enCurso++;
      }
    }
    return new UnidadReporte(
        numero,
        mapa.tituloDeUnidad(numero),
        completaron,
        enCurso,
        salon.size() - completaron - enCurso);
  }

  private List<AvanceDiario> avanceDelSalon(List<Long> ids, LocalDate hoy) {
    List<LocalDate> dias = new ArrayList<>();
    List<Integer> xp = new ArrayList<>();
    for (int i = DIAS_DEL_REPORTE - 1; i >= 0; i--) {
      LocalDate dia = hoy.minusDays(i);
      dias.add(dia);
      xp.add(
          ids.isEmpty()
              ? 0
              : reportes.sumarXpEntre(ids, inicioDelDia(dia), inicioDelDia(dia.plusDays(1))));
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

  private static List<String> observaciones(ReporteSalon reporte) {
    List<String> observaciones = new ArrayList<>();
    if (reporte.isVacio()) {
      return observaciones;
    }
    observaciones.add(
        reporte.activosSemana()
            + " de "
            + reporte.totalEstudiantes()
            + " estudiantes ganaron XP en los últimos 7 días ("
            + reporte.porcentajeActivos()
            + " %).");

    Integer variacion = reporte.variacionSemanal();
    if (variacion == null) {
      observaciones.add(
          reporte.xpSemana() > 0
              ? "El salón empezó a ganar XP esta semana; en los 7 días anteriores no tuvo"
                  + " actividad."
              : "El salón no registra XP en las dos últimas semanas.");
    } else if (variacion > 0) {
      observaciones.add(
          "La XP del salón subió " + variacion + " % respecto a los 7 días anteriores.");
    } else if (variacion < 0) {
      observaciones.add(
          "La XP del salón bajó " + (-variacion) + " % respecto a los 7 días anteriores.");
    } else {
      observaciones.add("La XP del salón se mantuvo igual que en los 7 días anteriores.");
    }

    reporte.unidades().stream()
        .filter(u -> u.enCurso() > 0)
        .max(Comparator.comparingInt(UnidadReporte::enCurso))
        .ifPresent(
            u ->
                observaciones.add(
                    "Unidad "
                        + u.numero()
                        + " «"
                        + u.titulo()
                        + "»: "
                        + u.enCurso()
                        + (u.enCurso() == 1
                            ? " estudiante la empezó y aún no la termina."
                            : " estudiantes la empezaron y aún no la terminan.")));

    Integer aciertos = reporte.porcentajeAciertos();
    if (aciertos != null && aciertos < ACIERTOS_MINIMOS) {
      observaciones.add(
          "El salón acierta el "
              + aciertos
              + " % de las preguntas: conviene repasar los temas antes de avanzar.");
    } else if (aciertos != null) {
      observaciones.add("El salón acierta el " + aciertos + " % de las preguntas.");
    }

    int apoyo = reporte.requierenApoyo().size();
    if (apoyo > 0) {
      observaciones.add(
          apoyo
              + (apoyo == 1
                  ? " estudiante necesita apoyo; revisa la lista de seguimiento."
                  : " estudiantes necesitan apoyo; revisa la lista de seguimiento."));
    }
    return observaciones;
  }

  private static int promedio(int suma, int cantidad) {
    return cantidad == 0 ? 0 : Math.round((float) suma / cantidad);
  }

  private static Instant inicioDelDia(LocalDate dia) {
    return dia.atStartOfDay(ZONA).toInstant();
  }
}
