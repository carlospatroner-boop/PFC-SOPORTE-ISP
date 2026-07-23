package ec.edu.uteq.soporte.ticketservice.web;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.service.TicketService;
import ec.edu.uteq.soporte.ticketservice.web.dto.ApiResponse;
import ec.edu.uteq.soporte.ticketservice.web.dto.CreateTicketRequest;
import ec.edu.uteq.soporte.ticketservice.web.dto.TicketResponse;
import ec.edu.uteq.soporte.ticketservice.web.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrato REST alineado con la Entrega 2 (Capitulo 7.3), con el prefijo /api/v1/.
 * La ruta de detalle incluye la zona porque forma parte de la clave primaria
 * compuesta en CockroachDB (ver ADR-0003).
 *
 * "authRole"/"authUserId"/"authZone" los pone AuthGatewayFilter tras validar el
 * access token contra auth-service; la decision de que puede hacer cada rol vive
 * en TicketService, no aqui (el controller solo reenvia lo que ya valido el filtro).
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestAttribute("authUserId") UUID clientId,
            @RequestAttribute("authRole") String role) {
        Ticket created = ticketService.createTicket(request, clientId, role);
        return ApiResponse.of(TicketResponse.from(created), "Ticket creado exitosamente");
    }

    @GetMapping
    public ApiResponse<List<TicketResponse>> listTickets(
            @RequestParam(required = false) Zone zone,
            @RequestParam(required = false) TicketStatus status,
            @RequestAttribute("authRole") String role,
            @RequestAttribute("authUserId") UUID userId,
            @RequestAttribute(required = false) Zone authZone) {
        List<TicketResponse> tickets = ticketService.listTickets(zone, status, role, userId, authZone).stream()
                .map(TicketResponse::from)
                .toList();
        return ApiResponse.of(tickets, "OK");
    }

    @GetMapping("/{zone}/{id}")
    public ApiResponse<TicketResponse> getTicket(
            @PathVariable Zone zone,
            @PathVariable UUID id,
            @RequestAttribute("authRole") String role,
            @RequestAttribute("authUserId") UUID userId,
            @RequestAttribute(required = false) Zone authZone) {
        Ticket ticket = ticketService.getTicket(zone, id, role, userId, authZone);
        return ApiResponse.of(TicketResponse.from(ticket), "OK");
    }

    @PatchMapping("/{zone}/{id}/status")
    public ApiResponse<TicketResponse> updateStatus(
            @PathVariable Zone zone,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestAttribute("authRole") String role,
            @RequestAttribute(required = false) Zone authZone) {
        Ticket updated = ticketService.updateStatus(zone, id, request.status(), role, authZone);
        return ApiResponse.of(TicketResponse.from(updated), "Estado actualizado");
    }

    @PostMapping("/{zone}/{id}/assign")
    public ApiResponse<TicketResponse> assignTechnician(
            @PathVariable Zone zone,
            @PathVariable UUID id,
            @RequestParam UUID technicianId,
            @RequestAttribute("authRole") String role,
            @RequestAttribute(required = false) Zone authZone) {
        Ticket updated = ticketService.assignTechnician(zone, id, technicianId, role, authZone);
        return ApiResponse.of(TicketResponse.from(updated), "Tecnico asignado");
    }
}
