package kanichi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kanichi.dto.UsuarioSesion;
import kanichi.repository.UsuarioRepository;
import kanichi.security.AutorizacionVigenteFilter;
import kanichi.security.LimiteIntentosLogin;
import kanichi.security.LimiteLoginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, LimiteIntentosLogin limite, UsuarioRepository usuarios) throws Exception {
    http.authorizeHttpRequests(
        permisos ->
            permisos
                .requestMatchers(
                    "/", "/registro", "/login", "/css/**", "/js/**", "/img/**", "/error")
                .permitAll()
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
                .requestMatchers("/estudiante/**")
                .hasRole("ESTUDIANTE")
                .requestMatchers("/onboarding", "/onboarding/**", "/inicio")
                .hasRole("ESTUDIANTE")
                .requestMatchers("/docente/**")
                .hasRole("DOCENTE")
                .requestMatchers("/cuenta/**")
                .authenticated()
                .anyRequest()
                .authenticated());

    http.formLogin(
        login ->
            login
                .loginPage("/login")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                .successHandler(this::redirigirDespuesDelLogin)
                .failureHandler(
                    (request, response, exception) -> {
                      limite.registrarFallo(request.getRemoteAddr());
                      log.info("evento=LOGIN_FALLIDO");
                      response.sendRedirect(request.getContextPath() + "/login?error");
                    })
                .permitAll());

    http.sessionManagement(
        sesion -> sesion.sessionFixation(fijacion -> fijacion.changeSessionId()));

    http.logout(
        logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .addLogoutHandler(
                    (request, response, autenticacion) -> {
                      if (autenticacion != null
                          && autenticacion.getPrincipal() instanceof UsuarioSesion usuario) {
                        log.info("evento=LOGOUT usuarioId={}", usuario.getId());
                      }
                    }));

    http.addFilterBefore(new LimiteLoginFilter(limite), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(new AutorizacionVigenteFilter(usuarios), AuthorizationFilter.class);
    return http.build();
  }

  private void redirigirDespuesDelLogin(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    UsuarioSesion usuario = (UsuarioSesion) authentication.getPrincipal();
    log.info("evento=LOGIN_EXITOSO usuarioId={} rol={}", usuario.getId(), usuario.getRol());
    response.sendRedirect(request.getContextPath() + usuario.destino());
  }
}
