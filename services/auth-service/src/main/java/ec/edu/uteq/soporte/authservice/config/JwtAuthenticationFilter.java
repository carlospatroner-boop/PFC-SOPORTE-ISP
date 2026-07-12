package ec.edu.uteq.soporte.authservice.config;

import ec.edu.uteq.soporte.authservice.exception.InvalidTokenException;
import ec.edu.uteq.soporte.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Parsea el access token (si viene) y, de ser valido, puebla el SecurityContext con
 * la autoridad ROLE_&lt;rol&gt; para que @PreAuthorize funcione en endpoints como
 * /api/v1/auth/admin/users. Si el token falta o es invalido, simplemente no autentica
 * -- no lanza error aqui; las rutas protegidas seran rechazadas mas adelante en la
 * cadena por authorizeHttpRequests, y las rutas publicas ni siquiera lo necesitan.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseAndValidate(header.substring("Bearer ".length()));
                String role = claims.get("role", String.class);
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException ignored) {
                // Token ausente/invalido: se deja sin autenticar, ver comentario de clase.
            }
        }
        filterChain.doFilter(request, response);
    }
}
