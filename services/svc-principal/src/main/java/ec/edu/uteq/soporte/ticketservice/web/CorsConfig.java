package ec.edu.uteq.soporte.ticketservice.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend estatico (carpeta /frontend, servido desde otro
 * puerto con Live Server o `python -m http.server`) pueda llamar a esta API mientras
 * no exista todavia un API Gateway delante (ver Entrega 2, Capitulo 4 -- Spring Cloud
 * Gateway). En produccion esto deberia restringirse al origen real del frontend en
 * lugar de "*".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
