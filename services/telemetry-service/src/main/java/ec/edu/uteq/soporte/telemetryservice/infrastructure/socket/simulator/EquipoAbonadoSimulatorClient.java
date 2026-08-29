package ec.edu.uteq.soporte.telemetryservice.infrastructure.socket.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ec.edu.uteq.soporte.telemetryservice.domain.LamportClock;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Cliente simulador de equipos del abonado (PE-U1, equipo ACC). No forma parte del arranque
 * normal del servicio (no es un {@code @Component} de Spring) -- es una clase con su propio
 * {@code main}, empaquetada en el mismo jar, para poder demostrar el canal de telemetria de
 * forma manual o desde un script de verificacion:
 *
 * <pre>
 *   java -cp telemetry-service.jar \
 *     ec.edu.uteq.soporte.telemetryservice.infrastructure.socket.simulator.EquipoAbonadoSimulatorClient \
 *     localhost 9500
 * </pre>
 *
 * <p>Cada instancia mantiene su PROPIO reloj de Lamport (independiente del reloj del servidor):
 * asi se demuestra que el timestamp que termina viendo el servidor es el resultado real de
 * aplicar la regla de Lamport sobre relojes de procesos distintos, no un numero inventado.
 */
public final class EquipoAbonadoSimulatorClient {

    private static final String[] ZONAS = {"QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"};
    private static final int[] NIVELES_SENAL_DBM = {-55, -62, -70, -78, -85};

    private EquipoAbonadoSimulatorClient() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 9500;
        int cantidadEquipos = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        int intervalosSegundos = args.length > 3 ? Integer.parseInt(args[3]) : 2;
        int iteraciones = args.length > 4 ? Integer.parseInt(args[4]) : 0; // 0 = infinito

        System.out.printf(
                "EquipoAbonadoSimulatorClient -> %s:%d, %d equipos, cada %ds%n",
                host, puerto, cantidadEquipos, intervalosSegundos);

        LamportClock reloj = new LamportClock();
        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();

        try (Socket socket = new Socket(host, puerto);
             OutputStream out = socket.getOutputStream()) {

            int vuelta = 0;
            while (iteraciones == 0 || vuelta < iteraciones) {
                for (int i = 0; i < cantidadEquipos; i++) {
                    String originId = "equipo-" + i;
                    String zona = ZONAS[random.nextInt(ZONAS.length)];
                    int nivelSenal = NIVELES_SENAL_DBM[random.nextInt(NIVELES_SENAL_DBM.length)];

                    ObjectNode payload = mapper.createObjectNode();
                    payload.put("signalLevelDbm", nivelSenal);

                    ObjectNode mensaje = mapper.createObjectNode();
                    mensaje.put("type", "EQUIPO");
                    mensaje.put("originId", originId);
                    mensaje.put("zone", zona);
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
