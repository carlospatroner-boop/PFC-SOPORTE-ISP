package ec.edu.uteq.soporte.ticketservice.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Confirma que los 3 instrumentos de D3.2 se registran con el nombre exacto que
 * exige la rubrica (promtool valida sobre esos nombres literales) -- sin
 * necesidad de un cluster CockroachDB real ni contexto de Spring.
 */
@ExtendWith(MockitoExtension.class)
class CrdbMetricsTest {

    @Mock
    private HikariDataSource dataSource;

    @Mock
    private HikariPoolMXBean poolMXBean;

    @Test
    void registersTheThreeRequiredInstrumentsWithExactNames() {
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(poolMXBean.getActiveConnections()).thenReturn(4);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrdbMetrics metrics = new CrdbMetrics(registry, dataSource);

        assertThat(registry.find("crdb_transaction_retries_total").counter()).isNotNull();
        assertThat(registry.find("crdb_query_duration_seconds").timer()).isNotNull();
        assertThat(registry.find("crdb_pool_active_connections").gauge()).isNotNull();
        assertThat(registry.find("crdb_pool_active_connections").gauge().value()).isEqualTo(4.0);

        metrics.incrementTransactionRetries();
        assertThat(registry.find("crdb_transaction_retries_total").counter().count()).isEqualTo(1.0);

        metrics.queryDurationTimer().record(Duration.ofMillis(42));
        assertThat(registry.find("crdb_query_duration_seconds").timer().count()).isEqualTo(1);
    }
}
