# Evidencias

## Nota sobre `captura_demo_paso3_notificacion.png` (retirada)

Una auditoría externa (`PFC_E4_Guia_Consolidacion_ACC.pdf`, sección 3) detectó que este archivo
—ya retirado de esta carpeta— se había construido pegando la barra de título de
`captura_demo_ticket_notificacion.png` sobre un tramo inferior recortado de la misma imagen
(`split_demo_capturas.py`, que se conserva en esta carpeta sin cambios, por transparencia sobre
qué se hizo y por qué). El objetivo original era cumplir con una observación de mostrar la
secuencia como capturas independientes, pero el resultado presentaba un montaje como si fuera una
ventana de terminal genuina — eso es lo que se corrigió, no el dato subyacente (la respuesta HTTP
real que mostraba nunca se alteró).

**Reemplazo**: [`captura_demo_ticket_notificacion_real.png`](captura_demo_ticket_notificacion_real.png),
generada el 2026-09-03 contra el *stack* de la Entrega 4 realmente en ejecución (no un montaje ni
una reconstrucción): se autenticó como `admin@soporte.local` vía `api-gateway`, se creó un ticket
real (`POST /api/v1/tickets`, ticket `142e484c-c7f4-4f32-b52a-876dd63752c5`), se esperó a que
`notification-service` lo consumiera de Kafka, y se consultó la notificación real resultante
(`GET /api/v1/notifications`). El texto de la imagen es una transcripción exacta de esa respuesta
real — renderizada con estilo de terminal para el manuscrito (`apps/web/shot_terminal.cjs`, no
conservado en el repo por ser un script de un solo uso), no una captura de pantalla del sistema
operativo, y así se declara en el manuscrito.

`captura_demo_ticket_notificacion.png` (la composición original de julio de 2026) se conserva sin
cambios: es una captura real de un solo trazo, no un montaje, y documenta la misma arquitectura de
demo en la Entrega 3 (rutas directas a los microservicios, antes de que existiera `api-gateway`).
