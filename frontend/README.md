# frontend — Panel de Tickets (equipo ACC)

Dashboard estático (HTML + CSS + JS vanilla, sin build step) que consume la API real de
`ticket-service`. Pensado para mostrar el sistema en vivo: KPIs, distribución de tickets por
zona/estado (ligado a la fragmentación de CockroachDB), y un tablero Kanban editable.

## Cómo correrlo contra el backend real

1. Levantar el cluster CockroachDB + `ticket-service` (ver `../db-cluster/README.md` y
   `../services/svc-principal/README.md`). El microservicio ya tiene CORS habilitado
   (`CorsConfig.java`) para que este frontend pueda llamarlo desde otro puerto.
2. Servir esta carpeta como archivos estáticos — cualquiera de estas opciones funciona:
   - Extensión **Live Server** de VS Code: clic derecho sobre `index.html` → "Open with Live Server".
   - `python3 -m http.server 5500` desde esta carpeta, y abrir `http://localhost:5500`.
3. Abrir el navegador. El indicador de conexión (abajo a la izquierda) debe ponerse verde
   ("Conectado a ticket-service"). Si sale en rojo, revisar que el microservicio esté corriendo
   en `localhost:8002` (la URL está fija en la primera línea útil de `app.js`, constante `API_BASE`).

## Modo demo sin backend (para practicar o si falla el cluster el día de la defensa)

Incluí `test/mock_server.py`, un servidor mínimo que imita el contrato REST real de
`ticket-service` (mismo formato de respuesta `{data, message, timestamp}`) con 3 tickets de
ejemplo en las 3 zonas. Sirve para:
- Iterar sobre el diseño del frontend sin tener Docker/CockroachDB levantado.
- Tener un plan B para la demo si el cluster real falla en el momento — **aclarando siempre
  al profesor que es un mock**, nunca presentarlo como el sistema real.

```bash
python3 test/mock_server.py
```

Corre en el mismo puerto (8002) que el microservicio real, así que no hace falta cambiar nada
en `app.js` — simplemente no tengas ambos corriendo al mismo tiempo.

## Qué muestra

- **Dashboard**: KPIs (total, abiertos, SLA vencido, resueltos/cerrados) y dos gráficos
  (Chart.js, vía CDN): distribución por zona — ligada 1:1 a las particiones
  `PARTITION BY LIST (zone)` del cluster — y distribución por estado.
- **Tablero de Tickets**: Kanban con las 6 columnas del ciclo de vida (NUEVO → ASIGNADO →
  EN_PROGRESO → ESCALADO/RESUELTO → CERRADO). Cada tarjeta permite cambiar el estado desde un
  selector, y un clic abre el detalle completo del ticket.
- Los tickets recién creados muestran "categoría/prioridad: pendiente IA" en vez de un valor —
  es un recordatorio visual honesto de que `ai-service` (Kafka) todavía no está conectado.

## Verificación ya realizada

Antes de entregarte esto, corrí una prueba automatizada (jsdom + un servidor mock) que carga
`index.html`, ejecuta `app.js` de verdad, y confirma que: los KPIs se calculan bien, las 6
columnas del tablero se arman, y crear un ticket vía el formulario actualiza el conteo en
pantalla. Lo que **no** pude probar desde este entorno es el flujo contra el CockroachDB real
(por eso conviene que lo primero que hagas sea abrirlo con el cluster ya levantado y confirmar
que el indicador de conexión se pone verde).

## Personalización rápida

- Colores de zona/estado: al principio de `app.js`, arrays `ZONES` y `STATUSES`.
- URL del backend: constante `API_BASE` en `app.js`.
- Frecuencia de auto-refresco: constante `POLL_INTERVAL_MS` (15 segundos por defecto).
