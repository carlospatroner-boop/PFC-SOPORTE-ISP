package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * A diferencia de SpringDataTicketRepository (package-private, oculto detras del puerto de
 * dominio TicketRepository), este SI es publico: Technician no es un concepto de dominio en
 * ticket-service (no hay TechnicianRepository ni casos de uso que lo consulten), asi que no
 * hay puerto que justificar -- el unico consumidor es
 * infrastructure/messaging/TechnicianSyncListener, en otro paquete de la misma capa de
 * infraestructura.
 */
public interface SpringDataTechnicianRepository extends JpaRepository<TechnicianJpaEntity, UUID> {
}
