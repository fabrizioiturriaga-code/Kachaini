package kanichi.dto;

import kanichi.model.LeccionDocente;

public class LeccionDocenteForm {
  private String titulo;
  private String contenido;
  private String pregunta;
  private String opcionA;
  private String opcionB;
  private String opcionC;
  private Integer respuestaCorrecta;

  public static LeccionDocenteForm desde(LeccionDocente leccion) {
    LeccionDocenteForm formulario = new LeccionDocenteForm();
    formulario.titulo = leccion.getTitulo();
    formulario.contenido = leccion.getContenido();
    formulario.pregunta = leccion.getPregunta();
    formulario.opcionA = leccion.getOpciones().get(0);
    formulario.opcionB = leccion.getOpciones().get(1);
    formulario.opcionC = leccion.getOpciones().get(2);
    formulario.respuestaCorrecta = leccion.getRespuestaCorrecta();
    return formulario;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getContenido() {
    return contenido;
  }

  public void setContenido(String contenido) {
    this.contenido = contenido;
  }

  public String getPregunta() {
    return pregunta;
  }

  public void setPregunta(String pregunta) {
    this.pregunta = pregunta;
  }

  public String getOpcionA() {
    return opcionA;
  }

  public void setOpcionA(String opcionA) {
    this.opcionA = opcionA;
  }

  public String getOpcionB() {
    return opcionB;
  }

  public void setOpcionB(String opcionB) {
    this.opcionB = opcionB;
  }

  public String getOpcionC() {
    return opcionC;
  }

  public void setOpcionC(String opcionC) {
    this.opcionC = opcionC;
  }

  public Integer getRespuestaCorrecta() {
    return respuestaCorrecta;
  }

  public void setRespuestaCorrecta(Integer respuestaCorrecta) {
    this.respuestaCorrecta = respuestaCorrecta;
  }
}
