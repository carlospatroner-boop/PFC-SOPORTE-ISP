# docs/diagrams — Diagramas de arquitectura (C4)

Los diagramas están escritos en Mermaid, embebidos directamente en Markdown — GitHub los
renderiza nativamente al ver el archivo, sin necesidad de exportar una imagen aparte:

- [`c4-nivel2-contenedores.md`](c4-nivel2-contenedores.md) — Vista de contenedores: los 5
  microservicios, CockroachDB, Kafka y MongoDB (arquitectura general actualizada tras la
  Entrega 3).
- [`c4-nivel3-particionado.md`](c4-nivel3-particionado.md) — Vista de componentes: zoom a cómo
  `ticket-service` accede a la tabla `tickets` particionada por `fecha_apertura` en CockroachDB
  (ver [ADR-0003](../adr/0003-sharding-policy.md)).
- [`db-schema.md`](db-schema.md) — Diagrama entidad-relación del esquema de `ticket_db`.

## Para incluirlos en el documento LaTeX

Los diagramas Mermaid no se pueden incrustar directamente en LaTeX. Exportar cada uno como PNG:

1. Ir a https://mermaid.live
2. Pegar el bloque de código Mermaid del archivo `.md` correspondiente.
3. Exportar como PNG (botón de descarga/export en la parte superior), 300 dpi si es posible.
4. Guardar el PNG en esta misma carpeta (por ejemplo `c4-nivel2.drawio.png`) y referenciarlo desde
   el manuscrito con `\includegraphics`.
