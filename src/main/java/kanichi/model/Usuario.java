package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, unique = true, length = 254)
  private String correo;

  @Column(name = "contrasena_hash", nullable = false, length = 60)
  private String contrasenaHash;

  @Column(name = "fecha_registro", nullable = false, updatable = false)
  private Instant fechaRegistro = Instant.now();

  protected Usuario() {}

  protected Usuario(String nombre, String correo, String contrasenaHash) {
    this.nombre = nombre;
    this.correo = correo;
    this.contrasenaHash = contrasenaHash;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public void actualizarNombreVisible(String nombre) {
    this.nombre = nombre;
  }

  public String getCorreo() {
    return correo;
  }

  public String getContrasenaHash() {
    return contrasenaHash;
  }

  public void actualizarContrasenaHash(String contrasenaHash) {
    this.contrasenaHash = contrasenaHash;
  }

  public Instant getFechaRegistro() {
    return fechaRegistro;
  }

  public abstract Rol getRol();

  public abstract boolean isAutorizado();
}
