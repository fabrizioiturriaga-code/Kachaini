package kanichi.service;

import java.util.ArrayList;
import java.util.List;
import kanichi.dto.RankingSalon;
import kanichi.dto.RankingSalon.Posicion;
import kanichi.model.Estudiante;
import kanichi.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ranking por XP del salón (mismo grado y sección). Los empates comparten puesto. */
@Service
public class RankingService {
  private final EstudianteRepository estudianteRepository;

  public RankingService(EstudianteRepository estudianteRepository) {
    this.estudianteRepository = estudianteRepository;
  }

  @Transactional(readOnly = true)
  public RankingSalon obtenerRankingDelSalon(Long estudianteId) {
    Estudiante actual =
        estudianteRepository
            .findById(estudianteId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el estudiante."));

    List<Estudiante> mejores =
        estudianteRepository.findTop10ByGradoAndSeccionOrderByXpDescNombreAsc(
            actual.getGrado(), actual.getSeccion());

    List<Posicion> posiciones = new ArrayList<>();
    for (int i = 0; i < mejores.size(); i++) {
      Estudiante estudiante = mejores.get(i);
      int puesto =
          i > 0 && estudiante.getXp() == mejores.get(i - 1).getXp()
              ? posiciones.get(i - 1).puesto()
              : i + 1;
      posiciones.add(
          new Posicion(
              puesto,
              estudiante.getNombre(),
              estudiante.getAvatar().getIcono(),
              estudiante.getXp(),
              estudiante.getId().equals(actual.getId())));
    }

    long miPosicion =
        estudianteRepository.countByGradoAndSeccionAndXpGreaterThan(
                actual.getGrado(), actual.getSeccion(), actual.getXp())
            + 1;
    return new RankingSalon(
        actual.getGrado(), actual.getSeccion(), posiciones, miPosicion, actual.getXp());
  }
}
