package kanichi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import kanichi.dto.NivelEstudiante;
import kanichi.dto.RecompensaObtenida;
import kanichi.dto.ResumenRecompensas;
import kanichi.dto.ResumenRecompensas.Entrada;
import kanichi.model.ActividadArrastre;
import kanichi.model.CambioLiga;
import kanichi.model.Coleccionable;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Lectura;
import kanichi.model.Liga;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.model.PracticaRealizada;
import kanichi.model.RecompensaPractica;
import kanichi.model.TipoDesafio;
import kanichi.model.TipoPractica;
import kanichi.repository.ActividadRepository;
import kanichi.repository.ActividadRepository.FilaHistorial;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.PracticaRealizadaRepository;
import kanichi.repository.RecompensaPracticaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecompensaService {
  public static final int PORCENTAJE_MINIMO = 80;
  public static final int XP_POR_PRACTICA = 15;
  static final int LIMITE_HISTORIAL = 30;

  private final EstudianteRepository estudianteRepository;
  private final PracticaRealizadaRepository practicaRepository;
  private final RecompensaPracticaRepository recompensaRepository;
  private final ActividadRepository actividadRepository;
  private final LigaService ligaService;
  private final Clock reloj;

  @Autowired
  public RecompensaService(
      EstudianteRepository estudianteRepository,
      PracticaRealizadaRepository practicaRepository,
      RecompensaPracticaRepository recompensaRepository,
      ActividadRepository actividadRepository,
      LigaService ligaService) {
    this(
        estudianteRepository,
        practicaRepository,
        recompensaRepository,
        actividadRepository,
        ligaService,
        Clock.systemUTC());
  }

  public RecompensaService(
      EstudianteRepository estudianteRepository,
      PracticaRealizadaRepository practicaRepository,
      RecompensaPracticaRepository recompensaRepository,
      ActividadRepository actividadRepository,
      LigaService ligaService,
      Clock reloj) {
    this.estudianteRepository = estudianteRepository;
    this.practicaRepository = practicaRepository;
    this.recompensaRepository = recompensaRepository;
    this.actividadRepository = actividadRepository;
    this.ligaService = ligaService;
    this.reloj = reloj;
  }

  @Transactional
  public RecompensaObtenida registrarPractica(
      Long estudianteId, TipoPractica tipo, String codigo, int unidad, int aciertos, int total) {
    if (total <= 0 || aciertos < 0 || aciertos > total) {
      throw new IllegalArgumentException("Calificación inválida.");
    }
    Estudiante estudiante =
        estudianteRepository
            .bloquearPorId(estudianteId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));
    Instant ahora = reloj.instant();
    practicaRepository.save(
        new PracticaRealizada(estudianteId, tipo, codigo, aciertos, total, ahora));

    int porcentaje = aciertos * 100 / total;
    if (aciertos * 100 < PORCENTAJE_MINIMO * total) {
      return RecompensaObtenida.sinRecompensa(porcentaje, false, false);
    }
    if (recompensaRepository.existsByEstudianteIdAndTipoAndCodigo(estudianteId, tipo, codigo)) {
      return RecompensaObtenida.sinRecompensa(porcentaje, true, true);
    }

    Coleccionable coleccionable =
        Coleccionable.deUnidad(unidad)
            .filter(c -> !recompensaRepository.existsByEstudianteIdAndColeccionable(estudianteId, c))
            .orElse(null);
    recompensaRepository.save(
        new RecompensaPractica(estudianteId, tipo, codigo, XP_POR_PRACTICA, coleccionable, ahora));
    estudiante.sumarXp(XP_POR_PRACTICA);
    estudianteRepository.registrarXp(estudianteId, XP_POR_PRACTICA, ahora);

    List<Logro> logros = new ArrayList<>();
    if (estudiante.otorgarLogro(Logro.PRACTICA_DESTACADA)) {
      logros.add(Logro.PRACTICA_DESTACADA);
    }
    logros.addAll(EvaluadorLogros.otorgarNuevos(estudiante, ahora));
    List<String> notificaciones =
        ligaService.evaluar(estudiante, ahora, true).stream().map(CambioLiga::mensaje).toList();
    return new RecompensaObtenida(
        porcentaje, true, false, XP_POR_PRACTICA, coleccionable, logros, notificaciones);
  }

  @Transactional(readOnly = true)
  public ResumenRecompensas resumen(Long estudianteId) {
    Estudiante estudiante =
        estudianteRepository
            .findById(estudianteId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));
    List<Coleccionable> coleccionables =
        recompensaRepository.coleccionablesDe(estudianteId).stream()
            .sorted(Comparator.naturalOrder())
            .toList();
    List<Entrada> historial =
        actividadRepository.historial(estudianteId, LIMITE_HISTORIAL).stream()
            .map(RecompensaService::entrada)
            .flatMap(Optional::stream)
            .toList();
    return new ResumenRecompensas(
        NivelEstudiante.de(estudiante.getXp()), coleccionables, historial);
  }

  static Optional<Entrada> entrada(FilaHistorial fila) {
    try {
      return Optional.of(
          switch (fila.origen()) {
            case "MISION" -> {
              MisionInicial mision = MisionInicial.valueOf(fila.referencia());
              yield new Entrada(
                  fila.fecha(), "🧭", "Misión completada: " + mision.getTitulo(), mision.getXp());
            }
            case "LECCION" -> {
              Leccion leccion = Leccion.valueOf(fila.referencia());
              yield new Entrada(
                  fila.fecha(), "📖", "Lección completada: " + leccion.getTitulo(), leccion.getXp());
            }
            case "INSIGNIA" -> {
              Logro logro = Logro.valueOf(fila.referencia());
              yield new Entrada(
                  fila.fecha(), logro.getIcono(), "Insignia obtenida: " + logro.getNombre(), null);
            }
            case "PRACTICA" -> entradaPractica(fila);
            case "DESAFIO" -> {
              TipoDesafio tipo = TipoDesafio.valueOf(fila.referencia());
              yield new Entrada(
                  fila.fecha(),
                  tipo.getIcono(),
                  "Desafío completado: " + tipo.getNombre(),
                  tipo.getXpRecompensa());
            }
            case "LIGA" -> {
              Liga liga = Liga.valueOf(fila.referencia());
              yield new Entrada(
                  fila.fecha(), liga.getIcono(), "Ascenso a " + liga.getNombre(), null);
            }
            default -> throw new IllegalArgumentException(fila.origen());
          });
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }

  private static Entrada entradaPractica(FilaHistorial fila) {
    String[] partes = fila.referencia().split(":", 2);
    TipoPractica tipo = TipoPractica.valueOf(partes[0]);
    String titulo =
        switch (tipo) {
          case LECTURA -> Lectura.porCodigo(partes[1]).map(Lectura::getTitulo).orElseThrow();
          case ARRASTRE ->
              ActividadArrastre.porCodigo(partes[1])
                  .map(ActividadArrastre::getTitulo)
                  .orElseThrow();
        };
    StringBuilder descripcion =
        new StringBuilder(tipo.getNombre()).append(" aprobada: ").append(titulo);
    if (fila.extra() != null) {
      Coleccionable coleccionable = Coleccionable.valueOf(fila.extra());
      descripcion
          .append(" · Coleccionable: ")
          .append(coleccionable.getIcono())
          .append(' ')
          .append(coleccionable.getNombre());
    }
    return new Entrada(fila.fecha(), "⭐", descripcion.toString(), fila.xp());
  }
}
