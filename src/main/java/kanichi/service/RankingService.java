package kanichi.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kanichi.dto.RankingSalon;
import kanichi.dto.RankingSalon.Posicion;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.model.Liga;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.EstudianteRepository.XpDelPeriodo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {
  public static final ZoneId ZONA = ZoneId.of("America/Lima");
  public static final int TAMANO_RANKING = 10;

  private final EstudianteRepository estudianteRepository;
  private final Clock reloj;

  @Autowired
  public RankingService(EstudianteRepository estudianteRepository) {
    this(estudianteRepository, Clock.systemUTC());
  }

  public RankingService(EstudianteRepository estudianteRepository, Clock reloj) {
    this.estudianteRepository = estudianteRepository;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public RankingSalon obtenerRankingDelSalon(Long estudianteId) {
    Estudiante actual =
        estudianteRepository
            .findById(estudianteId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));

    LocalDate lunes =
        LocalDate.now(reloj.withZone(ZONA))
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    Instant inicio = lunes.atStartOfDay(ZONA).toInstant();
    Instant fin = lunes.plusWeeks(1).atStartOfDay(ZONA).toInstant();
    Instant inicioSemanaPasada = lunes.minusWeeks(1).atStartOfDay(ZONA).toInstant();

    List<XpDelPeriodo> salon =
        estudianteRepository.xpDelSalonEntre(actual.getGrado(), actual.getSeccion(), inicio, fin);

    Map<Long, Liga> ligas = new HashMap<>();
    estudianteRepository
        .ligasDelSalon(actual.getGrado(), actual.getSeccion())
        .forEach(fila -> ligas.put(fila.getId(), fila.getLiga()));

    List<Posicion> posiciones = new ArrayList<>();
    int miXp = 0;
    long miPosicion = 1;
    for (int i = 0; i < salon.size(); i++) {
      XpDelPeriodo fila = salon.get(i);
      int xp = fila.getPuntos() == null ? 0 : fila.getPuntos();
      int puesto =
          i > 0 && xp == posiciones.get(i - 1).xp() ? posiciones.get(i - 1).puesto() : i + 1;
      boolean esActual = actual.getId().equals(fila.getId());
      if (esActual) {
        miXp = xp;
        miPosicion = puesto;
      }
      posiciones.add(
          new Posicion(
              puesto,
              fila.getNombre(),
              iconoDe(fila.getAvatar()),
              xp,
              esActual,
              ligas.get(fila.getId())));
    }

    return new RankingSalon(
        actual.getGrado(),
        actual.getSeccion(),
        List.copyOf(posiciones.subList(0, Math.min(TAMANO_RANKING, posiciones.size()))),
        miPosicion,
        miXp,
        estudianteRepository.sumarXpEntre(estudianteId, inicioSemanaPasada, inicio),
        actual.getXp(),
        lunes,
        lunes.plusDays(6),
        ligas.get(actual.getId()));
  }

  private static String iconoDe(String avatar) {
    try {
      return AvatarEstudiante.valueOf(avatar).getIcono();
    } catch (IllegalArgumentException | NullPointerException e) {
      return AvatarEstudiante.KAI.getIcono();
    }
  }
}