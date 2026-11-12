package kanichi.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import kanichi.model.CambioLiga;
import kanichi.model.Estudiante;
import kanichi.model.Liga;
import kanichi.repository.CambioLigaRepository;
import kanichi.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LigaService {
  static final int SEMANAS_A_REVISAR = 12;

  private final EstudianteRepository estudianteRepository;
  private final CambioLigaRepository cambioLigaRepository;
  private final Clock reloj;

  @Autowired
  public LigaService(
      EstudianteRepository estudianteRepository, CambioLigaRepository cambioLigaRepository) {
    this(estudianteRepository, cambioLigaRepository, Clock.systemUTC());
  }

  public LigaService(
      EstudianteRepository estudianteRepository,
      CambioLigaRepository cambioLigaRepository,
      Clock reloj) {
    this.estudianteRepository = estudianteRepository;
    this.cambioLigaRepository = cambioLigaRepository;
    this.reloj = reloj;
  }

  @Transactional
  public List<String> actualizarYNotificar(Long estudianteId) {
    Estudiante estudiante =
        estudianteRepository.bloquearPorId(estudianteId).orElseThrow(LigaService::noEncontrado);
    evaluar(estudiante, reloj.instant(), false);
    return consumirNotificaciones(estudianteId);
  }

  @Transactional
  public List<String> actualizarSalonYNotificar(Long estudianteId) {
    Estudiante estudiante =
        estudianteRepository.findById(estudianteId).orElseThrow(LigaService::noEncontrado);
    Instant ahora = reloj.instant();
    for (Estudiante companero :
        estudianteRepository.bloquearSalon(estudiante.getGrado(), estudiante.getSeccion())) {
      evaluar(companero, ahora, false);
    }
    return consumirNotificaciones(estudianteId);
  }

  public List<CambioLiga> evaluar(Estudiante estudiante, Instant ahora, boolean notificado) {
    LocalDate lunes = lunesDe(ahora);
    List<CambioLiga> cambios = new ArrayList<>();

    LocalDate semana = estudiante.getLigaSemana();
    if (semana != null && semana.isBefore(lunes)) {
      LocalDate limite = lunes.minusWeeks(SEMANAS_A_REVISAR);
      for (LocalDate w = semana.isBefore(limite) ? limite : semana;
          w.isBefore(lunes);
          w = w.plusWeeks(1)) {
        int xp = xpDeLaSemana(estudiante, w);
        Liga actual = estudiante.getLiga();
        Liga alcanzada = Liga.para(xp);
        if (alcanzada.ordinal() > actual.ordinal()) {
          cambios.add(cambiar(estudiante, alcanzada, ahora, notificado));
        } else if (xp < actual.getXpMinima()) {
          actual.anterior().ifPresent(l -> cambios.add(cambiar(estudiante, l, ahora, notificado)));
        }
      }
    }
    estudiante.evaluarLigaEnSemana(lunes);

    Liga alcanzada = Liga.para(xpDeLaSemana(estudiante, lunes));
    if (alcanzada.ordinal() > estudiante.getLiga().ordinal()) {
      cambios.add(cambiar(estudiante, alcanzada, ahora, notificado));
    }
    return cambios;
  }

  public static LocalDate lunesDe(Instant instante) {
    return instante
        .atZone(RankingService.ZONA)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  private List<String> consumirNotificaciones(Long estudianteId) {
    List<CambioLiga> pendientes =
        cambioLigaRepository.findByEstudianteIdAndNotificadoFalseOrderByIdAsc(estudianteId);
    pendientes.forEach(CambioLiga::marcarNotificado);
    return pendientes.stream().map(CambioLiga::mensaje).toList();
  }

  private int xpDeLaSemana(Estudiante estudiante, LocalDate lunes) {
    return estudianteRepository.sumarXpEntre(
        estudiante.getId(),
        lunes.atStartOfDay(RankingService.ZONA).toInstant(),
        lunes.plusWeeks(1).atStartOfDay(RankingService.ZONA).toInstant());
  }

  private CambioLiga cambiar(Estudiante estudiante, Liga nueva, Instant ahora, boolean notificado) {
    CambioLiga cambio =
        new CambioLiga(estudiante.getId(), estudiante.getLiga(), nueva, ahora, notificado);
    estudiante.cambiarLiga(nueva);
    return cambioLigaRepository.save(cambio);
  }

  private static IllegalStateException noEncontrado() {
    return new IllegalStateException("No se encontró el estudiante.");
  }
}
