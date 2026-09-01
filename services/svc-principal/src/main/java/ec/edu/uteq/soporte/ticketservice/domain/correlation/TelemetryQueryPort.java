package ec.edu.uteq.soporte.ticketservice.domain.correlation;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;

/**
 * Puerto (patron Repository/Strategy de dependencia) hacia el canal de telemetria de PE-U1
 * (servicio {@code telemetry-service}, ver docs/adr/0007-pe-u1-telemetria.md). El dominio
 * (ZonaVentanaTelemetriaStrategy) depende SOLO de esta interfaz -- nunca de gRPC directamente,
 * mismo criterio que EventPublisher para Kafka. El adaptador real vive en
 * infrastructure/grpc/TelemetryGrpcClientAdapter.java.
 */
public interface TelemetryQueryPort {

    /**
     * @return true si hay al menos un evento de tipo EQUIPO reportado en esa zona dentro de la
     *         ventana (evidencia real de telemetria, no solo la coincidencia de tickets). Si el
     *         canal no responde (caido, timeout), debe devolver false -- nunca lanzar una
     *         excepcion que revierta o bloquee la creacion del ticket (ver ADR-0004).
     */
    boolean hayEvidenciaDeAveria(Zone zone, long ventanaSegundos);
}
