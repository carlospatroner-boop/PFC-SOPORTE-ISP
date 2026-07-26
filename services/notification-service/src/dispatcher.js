// Logica pura de despacho multicanal: decide, para cada tipo de evento, que canal(es) usar y
// que mensaje enviar. Sin dependencias de Kafka/Mongo -- 100% testeable con node:test (ver
// test/dispatcher.test.js).
//
// Honestidad sobre el alcance (misma convencion que el clasificador basado en reglas de
// ai-service y el dataset sintetico de Spark): no hay un proveedor real de email/SMS/push
// configurado (no hay credenciales de SendGrid/Twilio/FCM). Esta funcion solo DECIDE el canal y
// el mensaje; kafkaConsumer.js los guarda en Mongo marcados como "simulated: true" -- nada se
// envia de verdad.
const EVENT_TICKET_CREATED = "ticket.created";
const EVENT_TICKET_STATUS_CHANGED = "ticket.status-changed";
const EVENT_TICKET_ASSIGNED = "ticket.assigned";

/**
 * @param {string} eventType uno de los topicos de Kafka consumidos
 * @param {object} event payload ya parseado (JSON plano publicado por ticket-service)
 * @returns {Array<{channel: string, message: string}>} una o mas notificaciones a despachar
 */
function dispatch(eventType, event) {
  switch (eventType) {
    case EVENT_TICKET_CREATED:
      return [
        {
          channel: "EMAIL",
          message: `Recibimos tu solicitud (ticket ${event.ticketId}). Te avisaremos cuando haya novedades.`,
        },
      ];

    case EVENT_TICKET_STATUS_CHANGED:
      return [
        {
          channel: "EMAIL",
          message: `Tu ticket ${event.ticketId} cambio de estado: ${event.oldStatus} -> ${event.newStatus}.`,
        },
      ];

    case EVENT_TICKET_ASSIGNED:
      // Una asignacion es mas urgente que un simple cambio de estado -- se notifica por 2
      // canales, decision genuinamente distinta a los otros dos casos (no "siempre EMAIL").
      return [
        {
          channel: "EMAIL",
          message: `Un tecnico fue asignado a tu ticket ${event.ticketId}.`,
        },
        {
          channel: "SMS",
          message: `Ticket ${event.ticketId}: tecnico asignado, pronto te contactara.`,
        },
      ];

    default:
      return [];
  }
}

module.exports = {
  dispatch,
  EVENT_TICKET_CREATED,
  EVENT_TICKET_STATUS_CHANGED,
  EVENT_TICKET_ASSIGNED,
};
