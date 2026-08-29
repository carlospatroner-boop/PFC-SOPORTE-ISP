package ec.edu.uteq.soporte.ticketservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad de dominio Ticket -- deliberadamente SIN anotaciones JPA (Modulo A, item 2 de la
 * guia de Entrega 4: "estas entidades no deben tener anotaciones JPA; las anotaciones se
 * colocan en clases de infraestructura"). El mapeo real a la tabla particionada `tickets`
 * de CockroachDB vive en infrastructure/persistence/TicketJpaEntity.java + TicketMapper.java.
 *
 * Mantiene el mismo shape de campos que el modelo fisico (ver ADR-0003: fragmentacion
 * horizontal por created_at) para que el mapeo sea directo, pero esta clase no sabe nada
 * de SQL, particiones ni el motor de persistencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    private OffsetDateTime createdAt;
    private UUID id;
    private Zone zone;
    private UUID clientId;
    private UUID technicianId;
    private Category category;
    private Priority priority;
    private TicketStatus status;
    private String description;
    private OffsetDateTime slaDeadline;
    private OffsetDateTime resolvedAt;
    private boolean slaBreached;
}
