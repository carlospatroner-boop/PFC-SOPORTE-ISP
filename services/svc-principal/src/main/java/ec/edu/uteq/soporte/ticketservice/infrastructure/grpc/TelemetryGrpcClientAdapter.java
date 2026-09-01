package ec.edu.uteq.soporte.ticketservice.infrastructure.grpc;

import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.EventosResponse;
import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.TelemetryServiceGrpc;
import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.ZonaWindowRequest;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.TelemetryQueryPort;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptador (implementa el puerto de dominio {@link TelemetryQueryPort}) hacia el gRPC real de
 * telemetry-service (PE-U1). Las clases generadas ({@code TelemetryServiceGrpc}, etc.) quedan
 * bajo el paquete "telemetryservice" porque {@code telemetry.proto} fija ese
 * {@code java_package} -- es el mismo contrato compartido, copiado sin cambios desde
 * services/telemetry-service/src/main/proto/telemetry.proto, no un error de paquete.
 */
@Component
public class TelemetryGrpcClientAdapter implements TelemetryQueryPort {

    private static final Logger LOGGER = Logger.getLogger(TelemetryGrpcClientAdapter.class.getName());

    private final ManagedChannel channel;
    private final TelemetryServiceGrpc.TelemetryServiceBlockingStub stub;

    public TelemetryGrpcClientAdapter(
            @Value("${telemetry.service.host:localhost}") String host,
            @Value("${telemetry.service.grpc-port:9095}") int puerto) {
        this.channel = ManagedChannelBuilder.forAddress(host, puerto).usePlaintext().build();
        this.stub = TelemetryServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public boolean hayEvidenciaDeAveria(Zone zone, long ventanaSegundos) {
        try {
            ZonaWindowRequest request = ZonaWindowRequest.newBuilder()
                    .setZone(zone.name())
                    .setWindowSeconds(ventanaSegundos)
                    .build();
            // El plazo se fija AQUI, en cada llamada -- withDeadlineAfter() calcula un punto
            // fijo en el tiempo (ahora + 3s) en el momento en que se invoca. Fijarlo una sola
            // vez en el constructor (bug real que se encontro probando esto en vivo) deja el
            // plazo vencido para siempre despues de los primeros 3 segundos de vida del bean.
            EventosResponse response = stub.withDeadlineAfter(3, TimeUnit.SECONDS).getEventosPorZona(request);
            return response.getEventsList().stream().anyMatch(e -> "EQUIPO".equals(e.getType()));
        } catch (StatusRuntimeException e) {
            // Canal caido o con timeout: se trata como "sin evidencia", nunca se propaga --
            // ver TelemetryQueryPort y ADR-0004 (nunca bloquear/revertir la creacion del ticket).
            LOGGER.log(Level.WARNING, "telemetry-service no respondio, se asume sin evidencia: " + e.getStatus(), e);
            return false;
        }
    }

    @PreDestroy
    public void cerrarCanal() {
        channel.shutdownNow();
    }
}
