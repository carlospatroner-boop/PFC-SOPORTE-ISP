package ec.edu.uteq.soporte.telemetryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PE-U1 (equipo ACC): canal de telemetria de equipos del abonado y latidos de nodo. El
 * arranque de Spring aqui solo sirve para exponer /actuator/health y levantar el servidor
 * gRPC embebido -- el servidor de sockets TCP (el canal de negocio real) lo levanta
 * {@code TelemetrySocketServer} via @PostConstruct. Ver docs/adr/0007-pe-u1-telemetria.md.
 */
@SpringBootApplication
public class TelemetryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
