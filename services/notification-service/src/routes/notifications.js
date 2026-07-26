// GET /api/v1/notifications?ticketId=... -- mismo rol de depuracion/demo que
// GET /api/v1/ai/classifications/{ticket_id} en ai-service: permite ver que se "envio" sin
// necesitar acceso directo a Mongo.
const express = require("express");
const { getCollection } = require("../db");

const router = express.Router();

router.get("/", async (req, res) => {
  const { ticketId } = req.query;
  const collection = await getCollection();
  const filter = ticketId ? { ticketId } : {};
  const docs = await collection.find(filter, { projection: { _id: 0 } }).sort({ createdAt: 1 }).toArray();
  res.json({ data: docs, message: "OK", timestamp: new Date().toISOString() });
});

module.exports = router;
