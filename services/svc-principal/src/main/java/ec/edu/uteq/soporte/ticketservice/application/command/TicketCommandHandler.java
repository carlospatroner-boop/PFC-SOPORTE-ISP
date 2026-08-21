package ec.edu.uteq.soporte.ticketservice.application.command;

/**
 * Patron Command: cada accion mutable sobre un ticket (crear, cambiar estado, asignar
 * tecnico) se representa como un objeto de comando (los "C" concretos: CreateTicketCommand,
 * UpdateTicketStatusCommand, AssignTechnicianCommand) y se ejecuta a traves de un manejador
 * dedicado que implementa esta interfaz.
 *
 * Antes del refactor, TicketController llamaba directamente a metodos con nombre
 * (ticketService.createTicket(...), .updateStatus(...), etc.) implementados todos en una
 * unica clase. Con Command, el controlador (presentation/TicketController) arma un objeto de
 * comando y se lo entrega a su manejador -- el invocador queda desacoplado de como se
 * ejecuta la accion, lo que permite en el futuro anadir logging, colas o reintentos
 * uniformes sin tocar el controlador.
 */
public interface TicketCommandHandler<C, R> {
    R handle(C command);
}
