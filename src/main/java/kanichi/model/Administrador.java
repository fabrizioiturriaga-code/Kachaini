package kanichi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "administradores")
public class Administrador extends Usuario {
  protected Administrador() {}

  public Administrador(String nombre, String correo, String contrasenaHash) {
    super(nombre, correo, contrasenaHash);
  }

  @Override
  public Rol getRol() {
    return Rol.ADMIN;
  }

  @Override
  public boolean isAutorizado() {
    return true;
  }
}
