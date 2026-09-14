# docs/diagrams — Diagramas de arquitectura (C4)

Los diagramas están escritos en Mermaid, embebidos directamente en Markdown — GitHub los
renderiza nativamente al ver el archivo, sin necesidad de exportar una imagen aparte:

- [`c4-nivel1-contexto.md`](c4-nivel1-contexto.md) — Vista de contexto: el sistema como una sola
  caja frente a sus tres tipos de usuario.
- [`c4-nivel2-contenedores.md`](c4-nivel2-contenedores.md) — Vista de contenedores: los 7
  microservicios, CockroachDB, Kafka y MongoDB (arquitectura general actualizada tras la
  Entrega 4).
- [`c4-nivel3-componentes.md`](c4-nivel3-componentes.md) — Vista de componentes: las 4 capas y
  6 patrones GoF reales de `ticket-service` (ver [ADR-0005](../adr/0005-patrones-gof.md)).
- [`particionado-tickets-cockroachdb.md`](particionado-tickets-cockroachdb.md) — Detalle de
  infraestructura (no es un nivel del modelo C4): cómo `ticket-service` accede a la tabla
  `tickets` particionada por `fecha_apertura` en CockroachDB (ver
  [ADR-0003](../adr/0003-sharding-policy.md)).
- [`db-schema.md`](db-schema.md) — Diagrama entidad-relación del esquema de `ticket_db`.

## Para incluirlos en el documento LaTeX

Los diagramas Mermaid no se pueden incrustar directamente en LaTeX. Exportar cada uno como PNG:

1. Ir a https://mermaid.live
2. Pegar el bloque de código Mermaid del archivo `.md` correspondiente.
3. Exportar como PNG (botón de descarga/export en la parte superior), 300 dpi si es posible.
4. Guardar el PNG en esta misma carpeta (por ejemplo `c4-nivel2.drawio.png`) y referenciarlo desde
   el manuscrito con `\includegraphics`.
