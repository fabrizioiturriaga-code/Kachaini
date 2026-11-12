package kanichi.service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kanichi.dto.ProgresoModulo;
import kanichi.dto.ProgresoModulo.EstadoMision;
import kanichi.dto.ResultadoMision;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-06: módulo inicial con misiones introductorias, recompensas y desbloqueo del mapa. */
@Service
public class ModuloInicialService {
  private final EstudianteRepository estudianteRepository;

  public ModuloInicialService(EstudianteRepository estudianteRepository) {
    this.estudianteRepository = estudianteRepository;
  }

  @Transactional(readOnly = true)
  public ProgresoModulo obtenerProgreso(Long estudianteId) {
    Estudiante estudiante =
        estudianteRepository.findById(estudianteId).orElseThrow(ModuloInicialService::noEncontrado);
    return progresoDe(estudiante);
  }

  @Transactional
  public ResultadoMision completar(Long estudianteId, MisionInicial mision, Integer respuesta) {
    Estudiante estudiante =
        estudianteRepository
            .bloquearPorId(estudianteId)
            .orElseThrow(ModuloInicialService::noEncontrado);

    if (estudiante.haCompletado(mision)) {
      return new ResultadoMision(0, List.of(), estudiante.isOnboardingCompletado());
    }
    if (!estaDisponible(estudiante, mision)) {
      throw new MisionNoPermitidaException("Completa primero la misión anterior.");
    }
    if (!mision.aceptaRespuesta(respuesta)) {
      throw new MisionNoPermitidaException(
          "Respuesta incorrecta. Revisa las lecciones y vuelve a intentarlo.");
    }

    estudiante.registrarMision(mision);
    estudianteRepository.registrarXp(estudianteId, mision.getXp(), Instant.now());
    List<Logro> logrosNuevos = new ArrayList<>();
    mision.getInsignia().filter(estudiante::otorgarLogro).ifPresent(logrosNuevos::add);

    boolean moduloCompletado =
        Arrays.stream(MisionInicial.values()).allMatch(estudiante::haCompletado);
    if (moduloCompletado) {
      estudiante.completarOnboarding();
      if (estudiante.otorgarLogro(Logro.EXPLORADOR)) {
        logrosNuevos.add(Logro.EXPLORADOR);
      }
    }
    return new ResultadoMision(mision.getXp(), logrosNuevos, moduloCompletado);
  }

  private static boolean estaDisponible(Estudiante estudiante, MisionInicial mision) {
    return mision.anterior().map(estudiante::haCompletado).orElse(true);
  }

  private static ProgresoModulo progresoDe(Estudiante estudiante) {
    List<EstadoMision> misiones =
        Arrays.stream(MisionInicial.values())
            .map(
                m ->
                    new EstadoMision(
                        m, estudiante.haCompletado(m), estaDisponible(estudiante, m)))
            .toList();
    List<Logro> logros =
        Arrays.stream(Logro.values()).filter(estudiante::tieneLogro).toList();
    return new ProgresoModulo(
        misiones, estudiante.getXp(), logros, estudiante.isOnboardingCompletado());
  }

  private static IllegalStateException noEncontrado() {
    return new IllegalStateException("No se encontró el estudiante autenticado.");
  }
}
