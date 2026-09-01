package ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryEvent;
import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryStore;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba del RPC {@code GetEventosPorZona} sin red real: servidor y cliente gRPC "in-process"
 * (io.grpc InProcessServerBuilder/InProcessChannelBuilder), rapida y determinista.
 */
class TelemetryGrpcServiceTest {

    private TelemetryStore store;
    private Server server;
    private ManagedChannel channel;
    private TelemetryServiceGrpc.TelemetryServiceBlockingStub stub;

    @BeforeEach
    void levantarServidorEnMemoria() throws Exception {
        store = new TelemetryStore();
        String nombreUnico = "telemetry-grpc-test-" + System.nanoTime();

        server = InProcessServerBuilder.forName(nombreUnico)
                .directExecutor()
                .addService(new TelemetryGrpcService(store))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(nombreUnico)
                .directExecutor()
                .build();

        stub = TelemetryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void apagarServidor() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void devuelve_los_eventos_de_la_zona_ordenados_por_lamport() {
        store.registrar(new TelemetryEvent(
                TelemetryEvent.TipoOrigen.NODO, "roach2", "QUEVEDO_CENTRO", 9L,
                System.currentTimeMillis(), "{\"status\":\"UP\"}"));
        store.registrar(new TelemetryEvent(
                TelemetryEvent.TipoOrigen.EQUIPO, "equipo-1", "QUEVEDO_CENTRO", 3L,
                System.currentTimeMillis(), "{\"signalLevelDbm\":-60}"));
        // Otra zona: no debe aparecer en la respuesta.
        store.registrar(new TelemetryEvent(
                TelemetryEvent.TipoOrigen.EQUIPO, "equipo-9", "QUEVEDO_SUR", 1L,
                System.currentTimeMillis(), "{}"));

        ZonaWindowRequest request = ZonaWindowRequest.newBuilder()
                .setZone("QUEVEDO_CENTRO")
                .setWindowSeconds(0)
                .build();

        EventosResponse response = stub.getEventosPorZona(request);

        assertThat(response.getEventsList()).hasSize(2);
        // Orden causal (por timestamp de Lamport), no orden de insercion: el evento con
        // lamport=3 (equipo-1) debe salir antes que el de lamport=9 (roach2).
        assertThat(response.getEvents(0).getOriginId()).isEqualTo("equipo-1");
        assertThat(response.getEvents(0).getLamportTimestamp()).isEqualTo(3L);
        assertThat(response.getEvents(1).getOriginId()).isEqualTo("roach2");
        assertThat(response.getEvents(1).getLamportTimestamp()).isEqualTo(9L);
    }

    @Test
    void una_zona_sin_eventos_devuelve_una_lista_vacia() {
        ZonaWindowRequest request = ZonaWindowRequest.newBuilder()
                .setZone("ZONA_INEXISTENTE")
                .setWindowSeconds(0)
                .build();

        EventosResponse response = stub.getEventosPorZona(request);

        assertThat(response.getEventsList()).isEmpty();
    }
}
