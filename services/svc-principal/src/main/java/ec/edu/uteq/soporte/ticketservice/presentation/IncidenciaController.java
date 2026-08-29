package ec.edu.uteq.soporte.ticketservice.presentation;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.IncidenciaRepository;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.ApiResponse;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.IncidenciaResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura del agrupamiento de tickets en Incidencias (Adicion 1 de la Ampliacion del Modulo G
 * -- ver docs/adr/0008-correl-incidencias.md). Anidado bajo /api/v1/tickets a proposito: asi
 * hereda la misma proteccion de AuthGatewayFilter (que solo filtra /api/v1/tickets/**) sin
 * tener que registrar un patron de URL nuevo.
 *
 * <p>Su consumidor principal es experimentos/analizar_correlacion.py, para comparar el
 * agrupamiento real contra la verdad de campo que escribe experimentos/inyector_averias.py.
 */
@RestController
@RequestMapping("/api/v1/tickets/incidencias")
public class IncidenciaController {

    private final IncidenciaRepository incidenciaRepository;

    public IncidenciaController(IncidenciaRepository incidenciaRepository) {
        this.incidenciaRepository = incidenciaRepository;
    }

    @GetMapping
    public ApiResponse<List<IncidenciaResponse>> listIncidencias(@RequestParam(required = false) Zone zone) {
        List<Incidencia> incidencias = zone != null
                ? incidenciaRepository.findByZone(zone)
                : incidenciaRepository.findAll();
        List<IncidenciaResponse> response = incidencias.stream().map(IncidenciaResponse::from).toList();
        return ApiResponse.of(response, "OK");
    }
}
