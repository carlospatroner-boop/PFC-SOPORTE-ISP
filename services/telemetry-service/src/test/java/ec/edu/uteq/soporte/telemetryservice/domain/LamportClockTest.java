package ec.edu.uteq.soporte.telemetryservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LamportClockTest {

    @Test
    void tick_avanza_el_reloj_en_uno_cada_vez() {
        LamportClock reloj = new LamportClock();

        assertThat(reloj.tick()).isEqualTo(1);
        assertThat(reloj.tick()).isEqualTo(2);
        assertThat(reloj.tick()).isEqualTo(3);
    }

    @Test
    void update_aplica_max_local_recibido_mas_uno() {
        LamportClock reloj = new LamportClock();
        reloj.tick(); // local = 1
        reloj.tick(); // local = 2

        // Recibe un mensaje con timestamp menor al local: gana el local.
        long resultado1 = reloj.update(1);
        assertThat(resultado1).isEqualTo(3); // max(2, 1) + 1

        // Recibe un mensaje con timestamp mayor al local: gana el recibido.
        long resultado2 = reloj.update(10);
        assertThat(resultado2).isEqualTo(11); // max(3, 10) + 1
    }

    @Test
    void es_monotono_bajo_acceso_concurrente() throws InterruptedException {
        LamportClock reloj = new LamportClock();
        int hilos = 8;
        int eventosPorHilo = 200;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        AtomicLong maximoVisto = new AtomicLong(0);

        for (int h = 0; h < hilos; h++) {
            pool.submit(() -> {
                for (int i = 0; i < eventosPorHilo; i++) {
                    long valor = reloj.tick();
                    maximoVisto.updateAndGet(actual -> Math.max(actual, valor));
                }
                listos.countDown();
            });
        }

        assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // Ningun tick() se pierde ni se repite: el valor final es exactamente la cuenta total.
        assertThat(reloj.valorActual()).isEqualTo((long) hilos * eventosPorHilo);
        assertThat(maximoVisto.get()).isEqualTo(reloj.valorActual());
    }
}
