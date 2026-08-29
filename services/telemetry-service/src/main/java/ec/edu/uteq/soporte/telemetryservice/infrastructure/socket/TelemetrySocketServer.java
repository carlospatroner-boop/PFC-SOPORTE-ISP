package ec.edu.uteq.soporte.telemetryservice.infrastructure.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.telemetryservice.domain.LamportClock;
import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryEvent;
import ec.edu.uteq.soporte.telemetryservice.domain.TelemetryStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Servidor TCP de telemetria (PE-U1, equipo ACC): acepta conexiones de los "clientes" (equipos
 * del abonado y latidos de nodo, ver el paquete {@code simulator}) y les asigna un timestamp de
 * Lamport a cada mensaje que reciben, antes de guardarlos en {@link TelemetryStore}.
 *
 * <p>Protocolo: una linea JSON por mensaje (delimitada por salto de linea), forma:
 * {@code {"type":"EQUIPO"|"NODO","originId":"...","zone":"...","payload":{...},"senderClock":N}}.
 * Un mensaje malformado no debe tumbar la conexion ni el servidor -- mismo criterio defensivo
 * que ya usan ai-service/notification-service al consumir Kafka: se registra y se sigue.
 */
@Component
public class TelemetrySocketServer {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySocketServer.class);

    private final LamportClock reloj = new LamportClock();
    private final TelemetryStore store;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int puerto;

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean corriendo = false;

    public TelemetrySocketServer(TelemetryStore store, @Value("${telemetry.socket.port:9500}") int puerto) {
        this.store = store;
        this.puerto = puerto;
    }

    @PostConstruct
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(puerto);
        pool = Executors.newCachedThreadPool();
        corriendo = true;
        Thread aceptador = new Thread(this::bucleAceptar, "telemetry-socket-acceptor");
        aceptador.setDaemon(true);
        aceptador.start();
        log.info("Servidor de sockets de telemetria escuchando en el puerto {}", puerto);
    }

    @PreDestroy
    public void detener() throws IOException {
        corriendo = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    private void bucleAceptar() {
        while (corriendo) {
            try {
                Socket cliente = serverSocket.accept();
                pool.submit(() -> manejarCliente(cliente));
            } catch (IOException e) {
                if (corriendo) {
                    log.error("Error aceptando una conexion de telemetria", e);
                }
                // Si !corriendo, es el cierre normal del ServerSocket en detener(): no es un error.
            }
        }
    }

    private void manejarCliente(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                procesarLinea(linea);
            }
        } catch (IOException e) {
            log.warn("Conexion de telemetria cerrada de forma anomala: {}", e.getMessage());
        }
    }

    private void procesarLinea(String linea) {
        if (linea.isBlank()) {
            return;
        }
        try {
            JsonNode nodo = mapper.readTree(linea);
            String tipoTexto = nodo.path("type").asText();
            TelemetryEvent.TipoOrigen tipo = TelemetryEvent.TipoOrigen.valueOf(tipoTexto);
            String originId = nodo.path("originId").asText();
            String zone = nodo.path("zone").asText();
            long senderClock = nodo.path("senderClock").asLong(0);
            String payloadJson = nodo.path("payload").toString();

            long lamport = reloj.update(senderClock);
            TelemetryEvent evento = new TelemetryEvent(
                    tipo, originId, zone, lamport, System.currentTimeMillis(), payloadJson);
            store.registrar(evento);
        } catch (Exception e) {
            // Un mensaje malformado no debe tumbar el canal completo -- se registra y se sigue
            // con el siguiente, mismo criterio que el resto de consumidores del sistema.
            log.warn("Mensaje de telemetria descartado por formato invalido: {} -- linea: {}",
                    e.getMessage(), linea);
        }
    }
}
