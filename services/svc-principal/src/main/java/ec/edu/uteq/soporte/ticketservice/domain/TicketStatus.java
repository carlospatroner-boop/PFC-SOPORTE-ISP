package ec.edu.uteq.soporte.ticketservice.domain;

/**
 * Estados del ticket segun la maquina de estados definida en la Entrega 2
 * (Figura 3.1: NUEVO -> ASIGNADO -> EN_PROGRESO -> RESUELTO -> CERRADO, con
 * transiciones adicionales de escalamiento).
 */
public enum TicketStatus {
    NUEVO,
    ASIGNADO,
    EN_PROGRESO,
    ESCALADO,
    RESUELTO,
    CERRADO
}
