package ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc;

import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryEvent;
import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryStore;
import io.grpc.stub.StreamObserver;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Implementacion del RPC {@code GetEventosPorZona} (PE-U1, equipo ACC) -- ver
 * {@code src/main/proto/telemetry.proto}. Es el punto de consulta del canal de telemetria: lee
 * de {@link TelemetryStore} (ya alimentado por {@code TelemetrySocketServer}) y devuelve los
 * eventos en orden causal (por timestamp de Lamport, no por orden de llegada).
 */
@GrpcService
public class TelemetryGrpcService extends TelemetryServiceGrpc.TelemetryServiceImplBase {

    private final TelemetryStore store;

    public TelemetryGrpcService(TelemetryStore store) {
        this.store = store;
    }

    @Override
    public void getEventosPorZona(ZonaWindowRequest request, StreamObserver<EventosResponse> responseObserver) {
        List<TelemetryEvent> eventos = store.consultar(request.getZone(), request.getWindowSeconds());

        EventosResponse.Builder builder = EventosResponse.newBuilder();
        for (TelemetryEvent evento : eventos) {
            builder.addEvents(TelemetryEventProto.newBuilder()
                    .setType(evento.tipo().name())
                    .setOriginId(evento.originId())
                    .setZone(evento.zone())
                    .setLamportTimestamp(evento.lamportTimestamp())
                    .setReceivedAtEpochMs(evento.recibidoEnEpochMs())
                    .setPayloadJson(evento.payloadJson())
                    .build());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
