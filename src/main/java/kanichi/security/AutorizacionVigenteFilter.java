package kanichi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import kanichi.dto.UsuarioSesion;
import kanichi.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AutorizacionVigenteFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(AutorizacionVigenteFilter.class);
  private final UsuarioRepository usuarioRepository;

  public AutorizacionVigenteFilter(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion principal) {
      boolean autorizado =
          usuarioRepository
              .findByCorreo(principal.getUsername())
              .filter(
                  usuario ->
                      usuario.isAutorizado()
                          && usuario.getRol() == principal.getRol()
                          && Objects.equals(usuario.getId(), principal.getId()))
              .isPresent();
      if (!autorizado) {
        var sesion = request.getSession(false);
        if (sesion != null) {
          sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        log.info("evento=SESION_REVOCADA usuarioId={}", principal.getId());
        response.sendRedirect(request.getContextPath() + "/login?revocada");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
