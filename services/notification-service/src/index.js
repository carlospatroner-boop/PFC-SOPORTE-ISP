// notification-service -- consumo de eventos de ticket-service y despacho multicanal simulado
// de notificaciones (equipo ACC).
//
// Responsabilidad (ver tabla de microservicios de la Entrega 2): consumir ticket.created /
// ticket.status-changed / ticket.assigned y notificar al cliente por el canal adecuado.
//
// Despacho simulado, no real -- ver dispatcher.js para la justificacion (no hay proveedor de
// email/SMS/push configurado).
const express = require("express");
const config = require("./config");
const { startConsumer } = require("./kafkaConsumer");
const notificationsRouter = require("./routes/notifications");

const app = express();

app.get("/health", (req, res) => {
  res.json({ status: "UP" });
});

app.use("/api/v1/notifications", notificationsRouter);

app.listen(config.port, () => {
  console.log(`notification-service arriba en el puerto ${config.port}`);
});

startConsumer().catch((err) => {
  console.error("No se pudo iniciar el consumidor de Kafka:", err);
});
