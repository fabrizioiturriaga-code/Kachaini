package kanichi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class LimiteLoginFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(LimiteLoginFilter.class);
  private final LimiteIntentosLogin limite;

  public LimiteLoginFilter(LimiteIntentosLogin limite) {
    this.limite = limite;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    boolean esLogin =
        "POST".equals(request.getMethod()) && "/login".equals(request.getServletPath());
    long espera = esLogin ? limite.segundosRestantes(request.getRemoteAddr()) : 0;
    if (espera > 0) {
      log.warn("evento=LOGIN_LIMITADO");
      response.setStatus(429);
      response.setHeader("Retry-After", Long.toString(espera));
      response.setContentType("text/html;charset=UTF-8");
      response
          .getWriter()
          .write(
              "<!doctype html><html lang=\"es\"><meta charset=\"utf-8\">"
                  + "<title>Espera para ingresar</title><main><h1>Demasiados intentos</h1>"
                  + "<p>Espera "
                  + espera
                  + " segundos antes de volver a intentarlo.</p>"
                  + "<a href=\""
                  + request.getContextPath()
                  + "/login\">Volver al inicio de sesión</a></main></html>");
      return;
    }
    chain.doFilter(request, response);
  }
}
