package ec.edu.uteq.soporte.telemetryservice.domain;

/**
 * Un evento de telemetria ya sellado con su timestamp de Lamport (PE-U1, equipo ACC). Puede
 * venir de un equipo del abonado (reporte de senal) o del latido de un nodo -- ver
 * {@link TipoOrigen}.
 */
public record TelemetryEvent(
        TipoOrigen tipo,
        String originId,
        String zone,
        long lamportTimestamp,
        long recibidoEnEpochMs,
        String payloadJson
) {
    public enum TipoOrigen {
        EQUIPO,
        NODO
    }
}
