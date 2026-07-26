// Configuracion via variables de entorno (mismo patron que ai-service/app/config.py y los
// application.yml de los servicios Java: defaults razonables para localhost, sobreescribibles
// en Docker).
module.exports = {
  port: parseInt(process.env.NOTIFICATION_SERVICE_PORT || "8003", 10),

  kafkaBootstrapServers: process.env.NOTIFICATION_SERVICE_KAFKA_BOOTSTRAP_SERVERS || "localhost:9092",
  kafkaClientId: "notification-service",
  kafkaGroupId: process.env.NOTIFICATION_SERVICE_KAFKA_GROUP_ID || "notification-service",
  topics: {
    ticketCreated: "ticket.created",
    ticketStatusChanged: "ticket.status-changed",
    ticketAssigned: "ticket.assigned",
  },

  mongoUri: process.env.NOTIFICATION_SERVICE_MONGO_URI || "mongodb://localhost:27017",
  mongoDb: process.env.NOTIFICATION_SERVICE_MONGO_DB || "notifications_db",
  mongoCollection: "notifications",
};
