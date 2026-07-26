// Cliente de MongoDB para notification-service. Una sola coleccion, "notifications": un
// documento por notificacion simulada despachada (ver dispatcher.js/kafkaConsumer.js).
const { MongoClient } = require("mongodb");
const config = require("./config");

let client = null;

async function getCollection() {
  if (!client) {
    client = new MongoClient(config.mongoUri, { serverSelectionTimeoutMS: 5000 });
    await client.connect();
  }
  return client.db(config.mongoDb).collection(config.mongoCollection);
}

module.exports = { getCollection };
