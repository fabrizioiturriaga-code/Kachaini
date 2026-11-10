package kanichi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kanichi.model.Rol;

public class RegistroForm {
  @NotBlank
  @Size(max = 100)
  private String nombre;

  @NotBlank
  @Email
  @Size(max = 254)
  private String correo;

  private String contrasena;
  private String confirmacion;
  @NotNull private Rol rol;
  private Integer grado;
  private String seccion;

  @Size(max = 100)
  private String area;

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String valor) {
    nombre = valor;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String valor) {
    correo = kanichi.validation.PoliticaCorreo.normalizar(valor);
  }

  public String getContrasena() {
    return contrasena;
  }

  public void setContrasena(String valor) {
    contrasena = valor;
  }

  public String getConfirmacion() {
    return confirmacion;
  }

  public void setConfirmacion(String valor) {
    confirmacion = valor;
  }

  public Rol getRol() {
    return rol;
  }

  public void setRol(Rol valor) {
    rol = valor;
  }

  public Integer getGrado() {
    return grado;
  }

  public void setGrado(Integer valor) {
    grado = valor;
  }

  public String getSeccion() {
    return seccion;
  }

  public void setSeccion(String valor) {
    seccion = valor;
  }

  public String getArea() {
    return area;
  }

  public void setArea(String valor) {
    area = valor;
  }
}
