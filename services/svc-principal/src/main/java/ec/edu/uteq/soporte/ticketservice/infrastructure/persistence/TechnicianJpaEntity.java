package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Copia local minima de un tecnico, sincronizada por evento desde auth-service (ver
 * infrastructure/messaging/TechnicianSyncListener.java) -- ticket-service no es dueno de
 * esta identidad, solo necesita que la fila exista para satisfacer
 * tickets_technician_id_fkey (ver db-cluster/scripts/init_db.sql) al asignar un tecnico a
 * un ticket. No expone puerto de dominio ni repositorio publico porque, a diferencia de
 * Ticket, ningun caso de uso de este servicio consulta ni lista tecnicos por si mismo.
 */
@Entity
@Table(name = "technicians")
public class TechnicianJpaEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String zone;

    private String specialty;

    @Column(nullable = false)
    private boolean active;

    protected TechnicianJpaEntity() {
        // Requerido por JPA.
    }

    public TechnicianJpaEntity(UUID id, String fullName, String zone, String specialty, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.zone = zone;
        this.specialty = specialty;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
