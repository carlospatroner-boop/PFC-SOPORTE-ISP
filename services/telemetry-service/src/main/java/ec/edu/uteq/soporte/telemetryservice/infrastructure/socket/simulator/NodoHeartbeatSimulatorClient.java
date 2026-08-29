package ec.edu.uteq.soporte.telemetryservice.infrastructure.socket.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ec.edu.uteq.soporte.telemetryservice.domain.LamportClock;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente simulador de latidos de nodo (PE-U1, equipo ACC) -- representa a los 3 nodos reales
 * del cluster CockroachDB (node1/node2/node3) reportando que siguen vivos. Misma logica que
 * {@link EquipoAbonadoSimulatorClient}: clase con su propio {@code main}, reloj de Lamport
 * propio e independiente del servidor.
 *
 * <pre>
 *   java -cp telemetry-service.jar \
 *     ec.edu.uteq.soporte.telemetryservice.infrastructure.socket.simulator.NodoHeartbeatSimulatorClient \
 *     localhost 9500
 * </pre>
 */
public final class NodoHeartbeatSimulatorClient {

    private static final String[] NODOS = {"roach1", "roach2", "roach3"};
    private static final String ZONA_CLUSTER = "QUEVEDO_CENTRO";

    private NodoHeartbeatSimulatorClient() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 9500;
        int intervalosSegundos = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        int iteraciones = args.length > 3 ? Integer.parseInt(args[3]) : 0; // 0 = infinito

        System.out.printf(
                "NodoHeartbeatSimulatorClient -> %s:%d, %d nodos, cada %ds%n",
                host, puerto, NODOS.length, intervalosSegundos);

        LamportClock reloj = new LamportClock();
        ObjectMapper mapper = new ObjectMapper();

        try (Socket socket = new Socket(host, puerto);
             OutputStream out = socket.getOutputStream()) {

            int vuelta = 0;
            while (iteraciones == 0 || vuelta < iteraciones) {
                for (String nodo : NODOS) {
                    ObjectNode payload = mapper.createObjectNode();
                    payload.put("status", "UP");

                    ObjectNode mensaje = mapper.createObjectNode();
                    mensaje.put("type", "NODO");
                    mensaje.put("originId", nodo);
                    mensaje.put("zone", ZONA_CLUSTER);
                    mensaje.set("payload", payload);
                    mensaje.put("senderClock", reloj.tick());

                    enviarLinea(out, mapper.writeValueAsString(mensaje));
                }
                vuelta++;
                Thread.sleep(intervalosSegundos * 1000L);
            }
        }
    }

    private static void enviarLinea(OutputStream out, String json) throws IOException {
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
