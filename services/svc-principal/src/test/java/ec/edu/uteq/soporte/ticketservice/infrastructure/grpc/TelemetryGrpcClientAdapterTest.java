package ec.edu.uteq.soporte.ticketservice.infrastructure.grpc;

import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.EventosResponse;
import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.TelemetryEventProto;
import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.TelemetryServiceGrpc;
import ec.edu.uteq.soporte.telemetryservice.infrastructure.grpc.ZonaWindowRequest;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de regresion de un bug real encontrado probando esto en vivo: el primer intento
 * fijaba el plazo del gRPC ({@code withDeadlineAfter}) UNA sola vez, en el constructor del
 * adaptador -- ese plazo es un punto fijo en el tiempo (ahora + 3s), no un plazo "por
 * llamada". Pasados los primeros 3 segundos de vida del bean, TODAS las llamadas siguientes
 * fallaban con DEADLINE_EXCEEDED, silenciosamente tratadas como "sin evidencia" (fail-safe,
 * ver TelemetryGrpcClientAdapter). Esta prueba levanta un servidor gRPC real (no un mock del
 * stub) y hace dos llamadas separadas por mas de 3 segundos, para que una regresion de este
 * bug especifico vuelva a fallar aqui.
 */
class TelemetryGrpcClientAdapterTest {

    private Server server;
    private TelemetryGrpcClientAdapter adapter;

    @AfterEach
    void apagar() {
        if (adapter != null) {
            adapter.cerrarCanal();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void unaSegundaLlamadaMasDe3SegundosDespuesTodaviaFunciona() throws Exception {
        int puerto = puertoLibre();
        server = ServerBuilder.forPort(puerto)
                .addService(new TelemetryServiceGrpc.TelemetryServiceImplBase() {
                    @Override
                    public void getEventosPorZona(ZonaWindowRequest request, StreamObserver<EventosResponse> obs) {
                        obs.onNext(EventosResponse.newBuilder()
                                .addEvents(TelemetryEventProto.newBuilder().setType("EQUIPO").build())
                                .build());
                        obs.onCompleted();
                    }
                })
                .build()
                .start();
        adapter = new TelemetryGrpcClientAdapter("localhost", puerto);

        assertThat(adapter.hayEvidenciaDeAveria(Zone.QUEVEDO_CENTRO, 900)).isTrue();

        // Mas de los 3s del plazo por llamada -- si el bug reaparece (plazo fijado una sola
        // vez en el constructor), esta segunda llamada fallaria con DEADLINE_EXCEEDED.
        Thread.sleep(3500);

        assertThat(adapter.hayEvidenciaDeAveria(Zone.QUEVEDO_CENTRO, 900)).isTrue();
    }

    private static int puertoLibre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
