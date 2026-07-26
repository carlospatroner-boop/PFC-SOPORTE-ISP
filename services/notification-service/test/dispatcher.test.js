const test = require("node:test");
const assert = require("node:assert");
const { dispatch, EVENT_TICKET_CREATED, EVENT_TICKET_STATUS_CHANGED, EVENT_TICKET_ASSIGNED } = require("../src/dispatcher");

test("ticket.created dispatches a single EMAIL notification", () => {
  const result = dispatch(EVENT_TICKET_CREATED, { ticketId: "t1", zone: "QUEVEDO_NORTE" });

  assert.strictEqual(result.length, 1);
  assert.strictEqual(result[0].channel, "EMAIL");
  assert.match(result[0].message, /t1/);
});

test("ticket.status-changed dispatches a single EMAIL notification mentioning both statuses", () => {
  const result = dispatch(EVENT_TICKET_STATUS_CHANGED, {
    ticketId: "t2",
    zone: "QUEVEDO_SUR",
    oldStatus: "NUEVO",
    newStatus: "ASIGNADO",
  });

  assert.strictEqual(result.length, 1);
  assert.strictEqual(result[0].channel, "EMAIL");
  assert.match(result[0].message, /NUEVO/);
  assert.match(result[0].message, /ASIGNADO/);
});

test("ticket.assigned dispatches EMAIL + SMS (more urgent, two channels)", () => {
  const result = dispatch(EVENT_TICKET_ASSIGNED, {
    ticketId: "t3",
    zone: "QUEVEDO_CENTRO",
    technicianId: "tech-1",
  });

  assert.strictEqual(result.length, 2);
  const channels = result.map((n) => n.channel).sort();
  assert.deepStrictEqual(channels, ["EMAIL", "SMS"]);
});

test("unknown event type dispatches nothing", () => {
  const result = dispatch("some.other.topic", { ticketId: "t4" });

  assert.strictEqual(result.length, 0);
});
