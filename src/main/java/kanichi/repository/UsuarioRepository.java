package kanichi.repository;

import java.util.Optional;
import kanichi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByCorreo(String correo);

  boolean existsByCorreo(String correo);
}
