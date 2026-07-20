# docs/diagrams — Diagramas de arquitectura (C4)

Acá van las imágenes exportadas desde mermaid.live (u otra herramienta), con estos nombres
exactos según la estructura de repositorio exigida por la guía de la Entrega 3:

- `c4_l1_contexto.png` — Vista de contexto: el sistema como caja única y sus usuarios externos.
- `c4_l2_contenedores.png` — Vista de contenedores: microservicios, bases de datos, Kafka, el
  cluster CockroachDB. **Este es el diagrama que armamos hoy con Mermaid** (arquitectura general).
- `c4_l3_componentes.png` — Vista de componentes: zoom a un servicio específico (por ejemplo,
  el zoom al cluster con las 3 particiones por zona que armamos hoy).

## Cómo generarlos

1. Ir a https://mermaid.live
2. Pegar el código Mermaid (los dos bloques que armamos en el chat).
3. Exportar como PNG (botón de descarga/export en la parte superior).
4. Guardar el archivo acá, en esta carpeta, con el nombre correspondiente de la lista de arriba.
