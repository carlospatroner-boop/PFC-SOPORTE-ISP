package ec.edu.uteq.soporte.telemetryservice.domain;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Reloj logico de Lamport (PE-U1, equipo ACC). Sin dependencias de framework a proposito: es
 * la pieza mas facil de probar de forma aislada y deterministica, y es el nucleo del "orden
 * causal" que la Guia de Reutilizacion asume que este canal ya provee.
 *
 * <p>Reglas clasicas de Lamport (1978):
 * <ol>
 *   <li>Antes de un evento local, el reloj avanza en uno ({@link #tick()}).</li>
 *   <li>Al recibir un mensaje con un timestamp ajeno, el reloj toma
 *       {@code max(local, recibido) + 1} ({@link #update(long)}), garantizando que el evento
 *       de recepcion queda causalmente despues tanto de su propia historia local como del envio
 *       que lo origino.</li>
 * </ol>
 */
public class LamportClock {

    private final AtomicLong valor = new AtomicLong(0);

    /** Evento local: avanza el reloj en uno y devuelve el nuevo valor. */
    public long tick() {
        return valor.incrementAndGet();
    }

    /**
     * Evento de recepcion: aplica la regla {@code max(local, recibido) + 1} de forma atomica y
     * devuelve el nuevo valor (el timestamp de Lamport que le corresponde a ESTE evento de
     * recepcion, no al mensaje recibido).
     */
    public long update(long timestampRecibido) {
        return valor.updateAndGet(actual -> Math.max(actual, timestampRecibido) + 1);
    }

    /** Valor actual sin avanzar el reloj -- solo para inspeccion/pruebas. */
    public long valorActual() {
        return valor.get();
    }
}
