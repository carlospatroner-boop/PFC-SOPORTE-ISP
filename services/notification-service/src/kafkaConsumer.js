// Consumidor de los 3 topicos que publica ticket-service (ver
// services/svc-principal/.../service/TicketService.java: publishTicketCreated/
// publishTicketStatusChanged/publishTicketAssigned). Por cada mensaje, despacha (dispatcher.js)
// una o mas notificaciones simuladas y las guarda en Mongo.
const { Kafka } = require("kafkajs");
const config = require("./config");
const { dispatch } = require("./dispatcher");
const { getCollection } = require("./db");
const logger = require("./logger");

async function handleMessage(topic, event) {
  const notifications = dispatch(topic, event);
  if (notifications.length === 0) {
    return;
  }
  const collection = await getCollection();
  const now = new Date().toISOString();
  const docs = notifications.map((n) => ({
    ticketId: event.ticketId,
    zone: event.zone,
    eventType: topic,
    channel: n.channel,
    message: n.message,
    simulated: true,
    createdAt: now,
  }));
  await collection.insertMany(docs);
}

async function startConsumer() {
  const kafka = new Kafka({
    clientId: config.kafkaClientId,
    brokers: [config.kafkaBootstrapServers],
  });
  const consumer = kafka.consumer({ groupId: config.kafkaGroupId });

  await consumer.connect();
  await consumer.subscribe({
    topics: Object.values(config.topics),
    fromBeginning: true,
  });

  await consumer.run({
    eachMessage: async ({ topic, message }) => {
      try {
        const event = JSON.parse(message.value.toString("utf-8"));
        await handleMessage(topic, event);
      } catch (err) {
        // Un mensaje malformado o un fallo puntual de Mongo no debe tumbar el consumidor --
        // se registra y se sigue con el siguiente (mismo criterio que
        // ai-service/app/kafka_consumer.py).
        logger.error("No se pudo procesar un mensaje de Kafka", { topic, error: err.message, stack: err.stack });
      }
    },
  });

  logger.info("notification-service escuchando", { topics: Object.values(config.topics) });
  return consumer;
}

module.exports = { startConsumer, handleMessage };
