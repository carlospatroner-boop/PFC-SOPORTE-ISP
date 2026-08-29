// Logging estructurado en JSON (Practica Experimental U5, item 2a: "logging estructurado
// (JSON) en todos los microservicios"). Sin dependencia nueva (nada de winston/pino): una
// linea de JSON por evento a stdout es suficiente para que cualquier colector de logs
// (o el propio "docker logs") lo parsee como estructurado, igual que ya hacen los 4
// servicios Java con LogstashEncoder (ver logback-spring.xml de cada uno).
const SERVICE = "notification-service";

function write(level, message, extra = {}) {
  const line = {
    "@timestamp": new Date().toISOString(),
    level,
    service: SERVICE,
    message,
    ...extra,
  };
  const out = level === "error" ? console.error : console.log;
  out(JSON.stringify(line));
}

module.exports = {
  info: (message, extra) => write("info", message, extra),
  error: (message, extra) => write("error", message, extra),
};
