package ec.edu.uteq.soporte.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada unico de la API distribuida (Modulo B, item 6 de la guia de Entrega 4):
 * la aplicacion web y la aplicacion movil consumen el backend exclusivamente a traves de
 * este gateway, nunca contra el puerto de un microservicio individual. Las reglas de
 * enrutamiento viven declarativas en application.yml -- no hay codigo Java de negocio aqui,
 * a proposito: un gateway con logica de dominio deja de ser un simple punto de entrada.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
