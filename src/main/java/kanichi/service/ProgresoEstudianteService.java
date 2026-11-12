package kanichi.service;

import java.time.Instant;
import kanichi.dto.ProgresoEstudiante;
import kanichi.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgresoEstudianteService {
  private final EstudianteRepository estudiantes;
  private final MapaMisionesService mapa;

  public ProgresoEstudianteService(EstudianteRepository estudiantes, MapaMisionesService mapa) {
    this.estudiantes = estudiantes;
    this.mapa = mapa;
  }

  @Transactional(readOnly = true)
  public ProgresoEstudiante obtener(Long estudianteId) {
    var estudiante =
        estudiantes
            .findById(estudianteId)
            .orElseThrow(
                () -> new IllegalStateException("No se encontró el estudiante autenticado."));
    var lecciones = estudiante.getLeccionesCompletadas();
    var intentos = estudiantes.contarIntentosEvaluados(estudianteId);
    return new ProgresoEstudiante(
        estudiante.getXp(),
        estudiante.cantidadMisionesCompletadas(),
        lecciones.size(),
        estudiante.rachaEn(Instant.now()).dias(),
        intentos.getAciertos(),
        intentos.getErrores(),
        mapa.obtenerMapa(estudiante.getGrado(), estudiante.isOnboardingCompletado(), lecciones));
  }
}
