package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "docentes")
public class Docente extends Usuario {
  @Column(nullable = false, length = 100)
  private String area;

  @Column(nullable = false)
  private boolean autorizado = false;

  protected Docente() {}

  public Docente(String nombre, String correo, String contrasenaHash, String area) {
    super(nombre, correo, contrasenaHash);
    this.area = area;
  }

  public String getArea() {
    return area;
  }

  public Rol getRol() {
    return Rol.DOCENTE;
  }

  public boolean isAutorizado() {
    return autorizado;
  }

  public void cambiarAutorizacion(boolean autorizado) {
    this.autorizado = autorizado;
  }
}
