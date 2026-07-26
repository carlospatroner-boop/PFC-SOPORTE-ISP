package ec.edu.uteq.soporte.ticketservice.integration;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integracion real (D3.1 de la rubrica de Entrega 3, Modulo A paso 6):
 * levanta un CockroachDB real en un contenedor Docker via Testcontainers -- no
 * depende de que el cluster de db-cluster/ este corriendo, ni usa una base
 * embebida (H2) que no reproduciria el comportamiento real de CockroachDB
 * (aislamiento serializable, tipos STRING/UUID nativos, etc).
 *
 * Dos pruebas:
 *  1) Un roundtrip save/findByTicketId contra el CockroachDB real, ejercitando
 *     el mismo repositorio que usa TicketService en produccion.
 *  2) Una prueba empirica de que el cluster aplica de verdad aislamiento
 *     SERIALIZABLE: dos transacciones concurrentes que leen y luego escriben la
 *     MISMA fila deben producir un error de conflicto de escritura (SQLSTATE
 *     40001) en al menos una de las dos -- el mismo tipo de error
 *     (WriteTooOldError / TransactionRetryError) que ya se observo en vivo en
 *     report-service (ver ReportEventListener.applyWithRetry) y que
 *     TicketService.saveWithRetry esta preparado para reintentar.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketRepositoryIntegrationTest {

    @Container
    static final GenericContainer<?> cockroach = new GenericContainer<>(DockerImageName.parse("cockroachdb/cockroach:latest-v23.2"))
            .withCommand("start-single-node", "--insecure")
            .withExposedPorts(26257, 8080)
            .waitingFor(Wait.forHttp("/health?ready=1").forPort(8080).withStartupTimeout(Duration.ofSeconds(90)));

    private static final String[] SCHEMA_STATEMENTS = {
            """
            CREATE TABLE IF NOT EXISTS technicians (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                full_name STRING NOT NULL,
                zone STRING NOT NULL,
                specialty STRING,
                active BOOL DEFAULT TRUE
            )
            """,
            // Mismo esquema que db-cluster/scripts/init_db.sql: PK (created_at, id)
            // particionada por rango de fecha (ver ADR-0003) + indice unico sobre
            // id para el punto de acceso real (TicketRepository.findByTicketId).
            """
            CREATE TABLE IF NOT EXISTS tickets (
                created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                id              UUID NOT NULL DEFAULT gen_random_uuid(),
                zone            STRING NOT NULL,
                client_id       UUID NOT NULL,
                technician_id   UUID REFERENCES technicians(id),
                category        STRING,
                priority        STRING,
                status          STRING NOT NULL DEFAULT 'NUEVO',
                description     STRING,
                sla_deadline    TIMESTAMPTZ,
                resolved_at     TIMESTAMPTZ,
                sla_breached    BOOL DEFAULT FALSE,
                PRIMARY KEY (created_at, id)
            )
            """,
            "CREATE UNIQUE INDEX IF NOT EXISTS tickets_id_key ON tickets (id)",
            "CREATE INDEX IF NOT EXISTS idx_tickets_zone ON tickets (zone)",
            "CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets (status)",
    };

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TicketRepositoryIntegrationTest::jdbcUrl);
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
    }

    private static String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/ticket_db?sslmode=disable"
                .formatted(cockroach.getHost(), cockroach.getMappedPort(26257));
    }

    // Corre antes de que Spring intente abrir el pool de conexiones (JUnit 5
    // ejecuta @BeforeAll estatico antes de instanciar el test, y ahi es donde el
    // TestContext framework de Spring evalua @DynamicPropertySource / crea el
    // ApplicationContext) -- la base "ticket_db" y su esquema deben existir para
    // ese momento, CockroachDB no la crea sola al conectar.
    @BeforeAll
    static void createDatabaseAndSchema() throws SQLException {
        String adminUrl = "jdbc:postgresql://%s:%d/defaultdb?sslmode=disable"
                .formatted(cockroach.getHost(), cockroach.getMappedPort(26257));
        try (Connection conn = DriverManager.getConnection(adminUrl, "root", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS ticket_db");
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), "root", "");
             Statement st = conn.createStatement()) {
            for (String ddl : SCHEMA_STATEMENTS) {
                st.execute(ddl);
            }
        }
    }

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void savesAndFindsTicketAgainstRealCockroachDb() {
        UUID id = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .zone(Zone.QUEVEDO_NORTE)
                .id(id)
                .clientId(UUID.randomUUID())
                .status(TicketStatus.NUEVO)
                .description("Prueba de integracion contra CockroachDB real (Testcontainers)")
                .createdAt(OffsetDateTime.now())
                .slaDeadline(OffsetDateTime.now().plusHours(24))
                .slaBreached(false)
                .build();

        ticketRepository.save(ticket);

        Optional<Ticket> found = ticketRepository.findByTicketId(id);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TicketStatus.NUEVO);
        assertThat(found.get().getZone()).isEqualTo(Zone.QUEVEDO_NORTE);
    }

    @Test
    void concurrentUpdatesOnSameRowProduceASerializableConflict() throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection setup = DriverManager.getConnection(jdbcUrl(), "root", "");
             PreparedStatement insert = setup.prepareStatement(
                     "INSERT INTO tickets (created_at, id, zone, client_id, status) "
                             + "VALUES (now(), ?, 'QUEVEDO_SUR', gen_random_uuid(), 'NUEVO')")) {
            insert.setObject(1, id);
            insert.executeUpdate();
        }

        // Ambos hilos leen la fila ANTES de que cualquiera escriba (barrera), para
        // forzar el patron clasico de conflicto bajo aislamiento serializable:
        // transaccion B lee un valor que transaccion A modifica antes de que B
        // confirme -- CockroachDB debe rechazar el commit de B (o de A) con un
        // error reintentable.
        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch proceedToWrite = new CountDownLatch(1);
        AtomicReference<SQLException> conflict = new AtomicReference<>();

        Runnable readThenWrite = () -> {
            try (Connection conn = DriverManager.getConnection(jdbcUrl(), "root", "")) {
                conn.setAutoCommit(false);
                try (PreparedStatement select = conn.prepareStatement("SELECT status FROM tickets WHERE id = ?")) {
                    select.setObject(1, id);
                    try (ResultSet rs = select.executeQuery()) {
                        rs.next();
                        rs.getString("status");
                    }
                }
                bothRead.countDown();
                proceedToWrite.await(10, TimeUnit.SECONDS);

                try (PreparedStatement update = conn.prepareStatement("UPDATE tickets SET status = 'EN_PROGRESO' WHERE id = ?")) {
                    update.setObject(1, id);
                    update.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conflict.compareAndSet(null, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(readThenWrite, "txn-A");
        Thread t2 = new Thread(readThenWrite, "txn-B");
        t1.start();
        t2.start();
        assertThat(bothRead.await(10, TimeUnit.SECONDS)).isTrue();
        proceedToWrite.countDown();
        t1.join(15_000);
        t2.join(15_000);

        // SQLException implementa Iterable<Throwable> (encadena getNextException()),
        // lo que hace ambiguo assertThat(...) entre el overload generico y el de
        // Iterable -- se castea a Exception para desambiguar.
        assertThat((Exception) conflict.get())
                .describedAs("al menos una de las dos transacciones concurrentes debio fallar por conflicto serializable")
                .isNotNull();
        assertThat(conflict.get().getSQLState()).isEqualTo("40001");
    }
}
