package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.event.TechnicianCreatedEvent;
import ec.edu.uteq.soporte.ticketservice.infrastructure.persistence.SpringDataTechnicianRepository;
import ec.edu.uteq.soporte.ticketservice.infrastructure.persistence.TechnicianJpaEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Otro lado de la Saga por coreografia (ver TechnicianEventPublisher en auth-service):
 * consume "technician.created" y sincroniza la tabla local `technicians`. Antes de esto, un
 * TECNICO dado de alta desde el panel de Admin de la web nunca llegaba aqui, y
 * AssignTechnicianHandler fallaba con una violacion de tickets_technician_id_fkey al
 * intentar asignarlo -- bug real, no hipotetico, encontrado probando la consola con una
 * cuenta de tecnico recien creada.
 *
 * Idempotente por diseno: si el mismo evento se reprocesa (redelivery de Kafka), el upsert
 * por id simplemente vuelve a escribir los mismos datos, no duplica filas.
 */
@Component
public class TechnicianSyncListener {

    private static final Logger LOGGER = Logger.getLogger(TechnicianSyncListener.class.getName());

    private final SpringDataTechnicianRepository technicianRepository;
    private final ObjectMapper objectMapper;

    public TechnicianSyncListener(SpringDataTechnicianRepository technicianRepository, ObjectMapper objectMapper) {
        this.technicianRepository = technicianRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "technician.created", groupId = "ticket-service")
    public void onTechnicianCreated(String payload) {
        try {
            TechnicianCreatedEvent event = objectMapper.readValue(payload, TechnicianCreatedEvent.class);
            upsert(event);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de technician.created: " + payload, e);
        }
    }

    private void upsert(TechnicianCreatedEvent event) {
        UUID id = UUID.fromString(event.technicianId());
        TechnicianJpaEntity technician = technicianRepository.findById(id)
                .orElse(new TechnicianJpaEntity(id, event.fullName(), event.zone(), null, true));
        technician.setFullName(event.fullName());
        technician.setZone(event.zone());
        technicianRepository.save(technician);
    }
}
