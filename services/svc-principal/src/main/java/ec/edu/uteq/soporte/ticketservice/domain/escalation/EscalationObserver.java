package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;

/**
 * Patron Observer: cada vez que EscalationScheduler (el sujeto) decide escalar un ticket,
 * notifica a todos los observadores registrados sin conocer sus implementaciones concretas.
 * Antes de este refactor, "reaccionar a un escalado" era una unica llamada a un logger en
 * linea dentro del propio scheduler -- agregar una nueva reaccion (una metrica, un evento de
 * integracion, una notificacion push) obligaba a editar esa clase. Con Observer, el scheduler
 * solo recorre la lista de {@link EscalationObserver} inyectada por Spring; agregar un
 * observador nuevo es agregar un @Component, no tocar el sujeto.
 */
public interface EscalationObserver {
    void onTicketEscalated(Ticket ticket, String motivo);
}
