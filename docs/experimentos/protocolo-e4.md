# Protocolo del experimento CORREL (Módulo G ampliado) — Entrega 4

Registro del entorno y el procedimiento exacto de la campaña de medición de la Adición 3
(`docs/adr/0008-correl-incidencias.md`), siguiendo el formato que exige la Guía de
reutilización, Sección 5.6 ("Qué debe quedar archivado"). Se congela **antes** de correr el
lote, no se reconstruye después.

## 1. Entorno de ejecución

| Campo | Valor |
|---|---|
| Fecha y hora (UTC) de esta corrida | Ver sección 4 (se registra por lote) |
| Commit evaluado | `31f1ef5` (rama `feature/entrega-4`) |
| Estado del árbol de trabajo | Ver `git status` adjunto en el mismo commit del lote |
| Host | Máquina de desarrollo del equipo (Windows) |
| Sistema operativo | Microsoft Windows 11 Pro, versión 10.0.22631 |
| Procesador | Intel(R) Core(TM) i7-10610U CPU @ 1.80GHz — 4 núcleos físicos / 8 lógicos |
| Memoria RAM | 16 924 368 896 bytes (≈ 15.8 GiB) |
| Motor de contenedores | Docker 28.5.2 (API 1.51) |
| Orquestación | `docker compose up -d --build` sobre `docker-compose.yml` de la raíz del repo |
| Topología de la base | Clúster CockroachDB de 3 nodos, imagen `cockroachdb/cockroach:latest-v23.2`, fragmentación de `tickets` por `fecha_apertura` (ADR-0003) |
| Mensajería | `apache/kafka:3.8.0`, un solo broker |
| Servicio de telemetría (PE-U1) | `telemetry-service`, servidor de sockets TCP en el puerto 9500, gRPC en 9095 |

## 2. Escenarios (Sección 5.4 de la guía de reutilización)

| Cód. | Escenario | Configuración |
|---|---|---|
| Esc-1 | Carga nominal | 50 usuarios durante 5 min, sin avería (ya cubierto por el escenario A de `resultados/locust/`, reutilizado para la Tabla 2 de ISO 25010; no se repite aquí) |
| Esc-2 | Avería masiva | 100 abonados afectados por una sola avería |
| Esc-3 | Avería mayor | 500 abonados afectados por una sola avería |
| Esc-4 | Dos averías simultáneas | Dos averías en zonas distintas al mismo tiempo, 100 abonados cada una (control obligatorio: revela fusión errónea, no es un caso a "ganar") |

Los escenarios 2, 3 y 4 se ejecutan bajo las tres configuraciones de `CORREL` (`c0`, `c1`,
`c2`), reiniciando `ticket-service` entre cada configuración para que la variable de entorno
tome efecto.

**Comprobaciones de control exigidas por la guía (Sección 5.4):**
- *Control negativo*: bajo `c0` con 500 abonados afectados, el sistema debe abrir del orden
  de 500 incidencias (una por ticket). Si no ocurre, la carga no está reproduciendo el
  fenómeno.
- *Escenario 4 obligatorio*: sirve para revelar fusión errónea (una estrategia que agrupa
  bien una avería puede fundir dos erróneamente), no para "pasar" o "fallar".

## 3. Escala real ejecutada en esta entrega

Por restricción de tiempo (cierre de comentarios del docente recibido el 5 de septiembre,
defensa la semana del 7 al 11), se ejecuta una **escala reducida** del protocolo completo
(que pide 10 repeticiones por escenario y los 3 escenarios de avería): los números exactos de
repeticiones, escenarios cubiertos y corridas totales de este lote se registran en la
Sección 4, una vez ejecutado. Metodología de análisis idéntica a la del protocolo completo
(Mann-Whitney U, tamaño del efecto de Vargha-Delaney, intervalo de confianza binomial para
las proporciones), aplicada a las muestras que sí se corrieron.

## 4. Registro del lote ejecutado

| Campo | Valor |
|---|---|
| Inicio (UTC) | 2026-09-08T02:43:54Z |
| Commit al momento de correr el lote | `31f1ef5d19167feb820ae4fa34e9c843c1d046de` |
| Árbol de trabajo | 23 archivos modificados/nuevos sin commitear (correcciones de esta misma revisión, ver mensaje de commit del lote) |
| Escenarios ejecutados | Esc-2 (avería masiva), Esc-3 (avería mayor), Esc-4 (dos averías simultáneas) — Esc-1 se omite: reutiliza el escenario A de `resultados/locust/`, ya corrido para la Tabla 2 de ISO 25010 |
| Escala real (declarada, reducida frente al protocolo completo) | 20 abonados (Esc-2, en vez de 100), 60 abonados (Esc-3, en vez de 500), 20+20 abonados en 2 zonas (Esc-4, en vez de 100+100) |
| Repeticiones por condición | 5 (en vez de las 10 del protocolo rector, mismo criterio de reducción ya declarado en `evaluacion_iso25010.md`) |
| Configuraciones | `c0`, `c1`, `c2` — las 3, sin reducción |
| Total de corridas | 45 (3 escenarios × 3 modos × 5 repeticiones), 60 filas de resultado (Esc-4 aporta 2 filas por corrida, una por zona) |
| Aislamiento entre corridas | `TRUNCATE incidencia_tickets, incidencias CASCADE` en `roach1` antes de cada corrida (no afecta la tabla `tickets` ni los datos de demo) |
| Script | `experimentos/correr_campana.py --reps 5` |
| Datos crudos | `experimentos/resultados/verdad_campo.csv`, `experimentos/resultados/correlacion_corridas.csv` |
| Análisis agregado | `experimentos/resultados/correlacion_analisis.json` |

**Qué queda pendiente para el protocolo completo:** subir a 10 repeticiones y a los conteos de
abonados de la Tabla 5.4 (100/500/100+100), y añadir el Esc-1 dedicado si el equipo decide no
reutilizar el escenario A de Locust. La metodología de análisis (Mann-Whitney U, Â₁₂ de
Vargha-Delaney, IC binomial) es la misma y no cambia al escalar.

## 5. Qué se archiva

| Qué | Dónde |
|---|---|
| Verdad de campo de cada corrida | `experimentos/resultados/verdad_campo.csv` |
| Incidencias observadas por corrida | `experimentos/resultados/correlacion_corridas.csv` |
| Análisis agregado (mediana, Mann-Whitney U, Â₁₂, IC binomial) | `experimentos/resultados/correlacion_analisis.json` |
| Este protocolo, congelado antes del lote | Este archivo |
