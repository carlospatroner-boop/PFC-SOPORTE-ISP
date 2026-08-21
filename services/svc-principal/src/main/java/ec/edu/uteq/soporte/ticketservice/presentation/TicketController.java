package ec.edu.uteq.soporte.ticketservice.presentation;

import ec.edu.uteq.soporte.ticketservice.application.TicketQueryService;
import ec.edu.uteq.soporte.ticketservice.application.command.AssignTechnicianCommand;
import ec.edu.uteq.soporte.ticketservice.application.command.AssignTechnicianHandler;
import ec.edu.uteq.soporte.ticketservice.application.command.CreateTicketCommand;
import ec.edu.uteq.soporte.ticketservice.application.command.CreateTicketHandler;
import ec.edu.uteq.soporte.ticketservice.application.command.UpdateTicketStatusCommand;
import ec.edu.uteq.soporte.ticketservice.application.command.UpdateTicketStatusHandler;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.ApiResponse;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.CreateTicketRequest;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.TicketResponse;
import ec.edu.uteq.soporte.ticketservice.presentation.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrato REST alineado con la Entrega 2 (Capitulo 7.3), con el prefijo /api/v1/. La ruta
 * de detalle usa solo el id del ticket -- desde que la fragmentacion paso a ser por
 * fecha_apertura (ver ADR-0003), la zona ya no forma parte de la clave primaria.
 *
 * "authRole"/"authUserId"/"authZone" los pone AuthGatewayFilter tras validar el access
 * token contra auth-service. El controlador arma el Command/Query correspondiente y se lo
 * entrega al manejador de aplicacion -- ya no decide el "que puede hacer cada rol", eso vive
 * en TicketAuthorization (patron Command, ver application/command/).
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final CreateTicketHandler createTicketHandler;
    private final UpdateTicketStatusHandler updateTicketStatusHandler;
    private final AssignTechnicianHandler assignTechnicianHandler;
    private final TicketQueryService ticketQueryService;

    public TicketController(
            CreateTicketHandler createTicketHandler,
            UpdateTicketStatusHandler updateTicketStatusHandler,
            AssignTechnicianHandler assignTechnicianHandler,
            TicketQueryService ticketQueryService) {
        this.createTicketHandler = createTicketHandler;
        this.updateTicketStatusHandler = updateTicketStatusHandler;
        this.assignTechnicianHandler = assignTechnicianHandler;
        this.ticketQueryService = ticketQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestAttribute("authUserId") UUID clientId,
            @RequestAttribute("authRole") String role) {
        CreateTicketCommand command = new CreateTicketCommand(
                request.zone(), request.title(), request.description(),
                request.contactPhone(), request.address(), clientId, role);
        Ticket created = createTicketHandler.handle(command);
        return ApiResponse.of(TicketResponse.from(created), "Ticket creado exitosamente");
    }

    @GetMapping
    public ApiResponse<List<TicketResponse>> listTickets(
            @RequestParam(required = false) Zone zone,
            @RequestParam(required = false) TicketStatus status,
            @RequestAttribute("authRole") String role,
            @RequestAttribute("authUserId") UUID userId,
            @RequestAttribute(required = false) Zone authZone) {
        List<TicketResponse> tickets = ticketQueryService.listTickets(zone, status, role, userId, authZone).stream()
                .map(TicketResponse::from)
                .toList();
        return ApiResponse.of(tickets, "OK");
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> getTicket(
            @PathVariable UUID id,
            @RequestAttribute("authRole") String role,
            @RequestAttribute("authUserId") UUID userId,
            @RequestAttribute(required = false) Zone authZone) {
        Ticket ticket = ticketQueryService.getTicket(id, role, userId, authZone);
        return ApiResponse.of(TicketResponse.from(ticket), "OK");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestAttribute("authRole") String role,
            @RequestAttribute(required = false) Zone authZone) {
        UpdateTicketStatusCommand command = new UpdateTicketStatusCommand(id, request.status(), role, authZone);
        Ticket updated = updateTicketStatusHandler.handle(command);
        return ApiResponse.of(TicketResponse.from(updated), "Estado actualizado");
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<TicketResponse> assignTechnician(
            @PathVariable UUID id,
            @RequestParam UUID technicianId,
            @RequestAttribute("authRole") String role,
            @RequestAttribute(required = false) Zone authZone) {
        AssignTechnicianCommand command = new AssignTechnicianCommand(id, technicianId, role, authZone);
        Ticket updated = assignTechnicianHandler.handle(command);
        return ApiResponse.of(TicketResponse.from(updated), "Tecnico asignado");
    }
}
