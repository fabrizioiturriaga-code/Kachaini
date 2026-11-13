package kanichi.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kanichi.model.Estudiante;
import kanichi.model.FactorXp;
import kanichi.model.MultiplicadorXp;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.MultiplicadorXpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MultiplicadorXpService implements CalculadoraXp {
  public static final int DIAS_MINIMOS = 1;
  public static final int DIAS_MAXIMOS = 30;

  private final MultiplicadorXpRepository multiplicadores;
  private final EstudianteRepository estudiantes;
  private final Clock reloj;

  @Autowired
  public MultiplicadorXpService(
      MultiplicadorXpRepository multiplicadores, EstudianteRepository estudiantes) {
    this(multiplicadores, estudiantes, Clock.systemUTC());
  }

  public MultiplicadorXpService(
      MultiplicadorXpRepository multiplicadores, EstudianteRepository estudiantes, Clock reloj) {
    this.multiplicadores = multiplicadores;
    this.estudiantes = estudiantes;
    this.reloj = reloj;
  }

  @Override
  public int calcular(Long estudianteId, int xpBase, Instant ahora) {
    return multiplicadores
        .findById(estudianteId)
        .filter(multiplicador -> multiplicador.estaActivoEn(ahora))
        .map(multiplicador -> multiplicador.getFactor().aplicar(xpBase))
        .orElse(xpBase);
  }

  @Transactional(readOnly = true)
  public Map<Long, MultiplicadorXp> activos(Collection<Long> estudianteIds) {
    if (estudianteIds.isEmpty()) {
      return Map.of();
    }
    Instant ahora = reloj.instant();
    return multiplicadores.findByEstudianteIdIn(estudianteIds).stream()
        .filter(multiplicador -> multiplicador.estaActivoEn(ahora))
        .collect(Collectors.toMap(MultiplicadorXp::getEstudianteId, Function.identity()));
  }

  @Transactional
  public int asignar(
      Long docenteId, int grado, List<Long> estudianteIds, FactorXp factor, Integer dias) {
    if (factor == null) {
      throw new IllegalArgumentException("Elige el multiplicador que quieres asignar.");
    }
    if (dias == null || dias < DIAS_MINIMOS || dias > DIAS_MAXIMOS) {
      throw new IllegalArgumentException(
          "La duración debe ser de " + DIAS_MINIMOS + " a " + DIAS_MAXIMOS + " días.");
    }
    Set<Long> ids = validarSeleccion(grado, estudianteIds);
    Instant inicio = reloj.instant();
    Instant fin = inicio.plus(Duration.ofDays(dias));
    for (Long id : ids) {
      multiplicadores
          .findById(id)
          .ifPresentOrElse(
              existente -> existente.actualizar(docenteId, factor, inicio, fin),
              () ->
                  multiplicadores.save(new MultiplicadorXp(id, docenteId, factor, inicio, fin)));
    }
    return ids.size();
  }

  @Transactional
  public int retirar(int grado, List<Long> estudianteIds) {
    Set<Long> ids = validarSeleccion(grado, estudianteIds);
    List<MultiplicadorXp> existentes = multiplicadores.findByEstudianteIdIn(ids);
    multiplicadores.deleteAll(existentes);
    return existentes.size();
  }

  private Set<Long> validarSeleccion(int grado, List<Long> estudianteIds) {
    Set<Long> ids =
        estudianteIds == null
            ? Set.of()
            : estudianteIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    if (ids.isEmpty()) {
      throw new IllegalArgumentException("Selecciona al menos un estudiante.");
    }
    List<Estudiante> encontrados = estudiantes.findAllById(ids);
    boolean todosDelGrado =
        encontrados.size() == ids.size()
            && encontrados.stream().allMatch(e -> Objects.equals(e.getGrado(), grado));
    if (!todosDelGrado) {
      throw new IllegalArgumentException("Solo puedes elegir estudiantes del grado seleccionado.");
    }
    return ids;
  }
}
