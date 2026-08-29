package ec.edu.uteq.soporte.telemetryservice.infrastructure.socket;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryEvent;
import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryStore;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba de integracion real (sin mocks): levanta {@link TelemetrySocketServer} en un puerto
 * efimero, un {@link Socket} de verdad le envia lineas JSON, y se verifica que
 * {@link TelemetryStore} termina con los eventos y sus timestamps de Lamport correctos.
 */
class TelemetrySocketServerIntegrationTest {

    private TelemetrySocketServer server;

    @AfterEach
    void apagarServidor() throws IOException {
        if (server != null) {
            server.detener();
        }
    }

    @Test
    void un_mensaje_valido_queda_registrado_con_timestamp_de_lamport() throws Exception {
        TelemetryStore store = new TelemetryStore();
        int puerto = puertoLibre();
        server = new TelemetrySocketServer(store, puerto);
        server.iniciar();

        try (Socket cliente = new Socket("localhost", puerto);
             OutputStream out = cliente.getOutputStream()) {
            enviar(out, "{\"type\":\"EQUIPO\",\"originId\":\"equipo-1\",\"zone\":\"QUEVEDO_CENTRO\","
                    + "\"payload\":{\"signalLevelDbm\":-60},\"senderClock\":5}");

            List<TelemetryEvent> eventos = esperarEventos(() -> store.consultar("QUEVEDO_CENTRO", 0), 1);
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).originId()).isEqualTo("equipo-1");
            assertThat(eventos.get(0).tipo()).isEqualTo(TelemetryEvent.TipoOrigen.EQUIPO);
            // update(5) sobre un reloj que arranca en 0: max(0, 5) + 1 = 6.
            assertThat(eventos.get(0).lamportTimestamp()).isEqualTo(6);
        }
    }

    @Test
    void mensajes_sucesivos_del_mismo_cliente_avanzan_el_reloj_causalmente() throws Exception {
        TelemetryStore store = new TelemetryStore();
        int puerto = puertoLibre();
        server = new TelemetrySocketServer(store, puerto);
        server.iniciar();

        try (Socket cliente = new Socket("localhost", puerto);
             OutputStream out = cliente.getOutputStream()) {
            enviar(out, "{\"type\":\"NODO\",\"originId\":\"roach1\",\"zone\":\"QUEVEDO_CENTRO\","
                    + "\"payload\":{\"status\":\"UP\"},\"senderClock\":1}");
            enviar(out, "{\"type\":\"NODO\",\"originId\":\"roach1\",\"zone\":\"QUEVEDO_CENTRO\","
                    + "\"payload\":{\"status\":\"UP\"},\"senderClock\":2}");

            List<TelemetryEvent> eventos = esperarEventos(() -> store.consultar("QUEVEDO_CENTRO", 0), 2);
            assertThat(eventos).hasSize(2);
            assertThat(eventos.get(0).lamportTimestamp())
                    .isLessThan(eventos.get(1).lamportTimestamp());
        }
    }

    @Test
    void un_mensaje_malformado_no_tumba_el_canal() throws Exception {
        TelemetryStore store = new TelemetryStore();
        int puerto = puertoLibre();
        server = new TelemetrySocketServer(store, puerto);
        server.iniciar();

        try (Socket cliente = new Socket("localhost", puerto);
             OutputStream out = cliente.getOutputStream()) {
            enviar(out, "esto no es JSON en absoluto");
            enviar(out, "{\"type\":\"EQUIPO\",\"originId\":\"equipo-2\",\"zone\":\"QUEVEDO_SUR\","
                    + "\"payload\":{},\"senderClock\":1}");

            List<TelemetryEvent> eventos = esperarEventos(() -> store.consultar("QUEVEDO_SUR", 0), 1);
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).originId()).isEqualTo("equipo-2");
        }
    }

    private static void enviar(OutputStream out, String json) throws IOException {
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static int puertoLibre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * El servidor procesa cada linea en un hilo aparte (ver TelemetrySocketServer), asi que
     * hay una latencia minima e inevitable entre "el cliente termino de escribir" y "el
     * evento ya esta en el store". Se espera activamente en vez de un Thread.sleep fijo, con
     * un tope de 5s para no dejar la prueba colgada si algo realmente esta roto.
     */
    private static List<TelemetryEvent> esperarEventos(Supplier<List<TelemetryEvent>> consulta, int cantidadEsperada)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        List<TelemetryEvent> ultimo = consulta.get();
        while (ultimo.size() < cantidadEsperada && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
            ultimo = consulta.get();
        }
        return ultimo;
    }
}
