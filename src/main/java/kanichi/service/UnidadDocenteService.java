package kanichi.service;

import java.time.Clock;
import java.util.List;
import kanichi.dto.LeccionDocenteForm;
import kanichi.dto.UnidadesDocente.Resumen;
import kanichi.model.LeccionDocente;
import kanichi.model.UnidadDocente;
import kanichi.repository.LeccionDocenteRepository;
import kanichi.repository.UnidadDocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UnidadDocenteService {
  public static final int MAXIMO_LECCIONES = 10;

  private final UnidadDocenteRepository unidadRepository;
  private final LeccionDocenteRepository leccionRepository;
  private final Clock reloj;

  @Autowired
  public UnidadDocenteService(
      UnidadDocenteRepository unidadRepository, LeccionDocenteRepository leccionRepository) {
    this(unidadRepository, leccionRepository, Clock.systemUTC());
  }

  public UnidadDocenteService(
      UnidadDocenteRepository unidadRepository,
      LeccionDocenteRepository leccionRepository,
      Clock reloj) {
    this.unidadRepository = unidadRepository;
    this.leccionRepository = leccionRepository;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public List<Resumen> deDocente(Long docenteId) {
    return unidadRepository.findByDocenteIdOrderByIdAsc(docenteId).stream()
        .map(unidad -> new Resumen(unidad, leccionRepository.countByUnidadId(unidad.getId())))
        .toList();
  }

  @Transactional
  public Long crear(
      Long docenteId, Integer grado, String titulo, LeccionDocenteForm primeraLeccion) {
    int gradoValido = validarGrado(grado);
    String tituloValido = texto(titulo, UnidadDocente.MAXIMO_TITULO, "el título de la unidad");
    LeccionDocente.Datos datos = validar(primeraLeccion);
    UnidadDocente unidad =
        unidadRepository.save(
            new UnidadDocente(docenteId, gradoValido, tituloValido, reloj.instant()));
    leccionRepository.save(new LeccionDocente(unidad.getId(), 1, datos));
    return unidad.getId();
  }

  @Transactional(readOnly = true)
  public UnidadDocente unidad(Long docenteId, Long unidadId) {
    return propia(docenteId, unidadId);
  }

  @Transactional(readOnly = true)
  public List<LeccionDocente> lecciones(Long docenteId, Long unidadId) {
    return leccionRepository.findByUnidadIdOrderByOrdenAsc(propia(docenteId, unidadId).getId());
  }

  @Transactional
  public void actualizarUnidad(Long docenteId, Long unidadId, Integer grado, String titulo) {
    UnidadDocente unidad = propia(docenteId, unidadId);
    unidad.actualizar(
        validarGrado(grado),
        texto(titulo, UnidadDocente.MAXIMO_TITULO, "el título de la unidad"),
        reloj.instant());
  }

  @Transactional
  public Long agregarLeccion(Long docenteId, Long unidadId, LeccionDocenteForm formulario) {
    UnidadDocente unidad = propia(docenteId, unidadId);
    LeccionDocente.Datos datos = validar(formulario);
    long cantidad = leccionRepository.countByUnidadId(unidadId);
    if (cantidad >= MAXIMO_LECCIONES) {
      throw new IllegalArgumentException(
          "Una unidad puede tener como máximo " + MAXIMO_LECCIONES + " lecciones.");
    }
    LeccionDocente leccion =
        leccionRepository.save(new LeccionDocente(unidadId, (int) cantidad + 1, datos));
    unidad.marcarActualizada(reloj.instant());
    return leccion.getId();
  }

  @Transactional(readOnly = true)
  public LeccionDocente leccion(Long docenteId, Long unidadId, Long leccionId) {
    propia(docenteId, unidadId);
    return deLaUnidad(unidadId, leccionId);
  }

  @Transactional
  public void actualizarLeccion(
      Long docenteId, Long unidadId, Long leccionId, LeccionDocenteForm formulario) {
    UnidadDocente unidad = propia(docenteId, unidadId);
    LeccionDocente leccion = deLaUnidad(unidadId, leccionId);
    leccion.actualizar(validar(formulario));
    unidad.marcarActualizada(reloj.instant());
  }

  @Transactional
  public void eliminarLeccion(Long docenteId, Long unidadId, Long leccionId) {
    UnidadDocente unidad = propia(docenteId, unidadId);
    LeccionDocente eliminada = deLaUnidad(unidadId, leccionId);
    List<LeccionDocente> lecciones = leccionRepository.findByUnidadIdOrderByOrdenAsc(unidadId);
    if (lecciones.size() <= 1) {
      throw new IllegalArgumentException("La unidad debe tener al menos una lección.");
    }
    leccionRepository.delete(eliminada);
    int orden = 1;
    for (LeccionDocente leccion : lecciones) {
      if (!leccion.getId().equals(eliminada.getId())) {
        leccion.moverA(orden++);
      }
    }
    unidad.marcarActualizada(reloj.instant());
  }

  public static LeccionDocente.Datos validar(LeccionDocenteForm formulario) {
    if (formulario == null) {
      throw new IllegalArgumentException("Completa los datos de la lección.");
    }
    String titulo = texto(formulario.getTitulo(), LeccionDocente.MAXIMO_TITULO, "el título de la lección");
    String contenido =
        texto(formulario.getContenido(), LeccionDocente.MAXIMO_CONTENIDO, "el contenido de la lección");
    String pregunta = texto(formulario.getPregunta(), LeccionDocente.MAXIMO_PREGUNTA, "la pregunta");
    List<String> opciones =
        List.of(
            texto(formulario.getOpcionA(), LeccionDocente.MAXIMO_OPCION, "la opción 1"),
            texto(formulario.getOpcionB(), LeccionDocente.MAXIMO_OPCION, "la opción 2"),
            texto(formulario.getOpcionC(), LeccionDocente.MAXIMO_OPCION, "la opción 3"));
    if (opciones.stream().map(String::toLowerCase).distinct().count() < LeccionDocente.OPCIONES) {
      throw new IllegalArgumentException("Las tres opciones deben ser distintas.");
    }
    Integer respuesta = formulario.getRespuestaCorrecta();
    if (respuesta == null || respuesta < 0 || respuesta >= LeccionDocente.OPCIONES) {
      throw new IllegalArgumentException("Marca cuál de las tres opciones es la correcta.");
    }
    return new LeccionDocente.Datos(titulo, contenido, pregunta, opciones, respuesta);
  }

  private UnidadDocente propia(Long docenteId, Long unidadId) {
    return unidadRepository
        .findById(unidadId)
        .filter(unidad -> unidad.esDe(docenteId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private LeccionDocente deLaUnidad(Long unidadId, Long leccionId) {
    return leccionRepository
        .findById(leccionId)
        .filter(leccion -> leccion.getUnidadId().equals(unidadId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static int validarGrado(Integer grado) {
    if (grado == null || !ProgresoDocenteService.GRADOS.contains(grado)) {
      throw new IllegalArgumentException("Elige un grado de 1.° a 5.° de secundaria.");
    }
    return grado;
  }

  private static String texto(String valor, int maximo, String campo) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("Escribe " + campo + ".");
    }
    String limpio = valor.strip();
    if (limpio.length() > maximo) {
      throw new IllegalArgumentException(
          "Escribe " + campo + " con " + maximo + " caracteres como máximo.");
    }
    return limpio;
  }
}
