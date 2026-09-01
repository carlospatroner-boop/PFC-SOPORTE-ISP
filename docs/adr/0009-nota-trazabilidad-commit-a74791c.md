# ADR-0009: Nota de trazabilidad — commit a74791c (refactor de 4 capas)

## Estado
Aceptado — Entrega 4 (Agosto 2026)

## Contexto
El commit `a74791c` ("refactor: reestructurar backend en 4 capas + Repository/Factory
Method/Strategy/Command/Chain of Responsibility", 20/08/2026) reporta 1.655 archivos
modificados, +2.492/-227.313 líneas en su diff. Ese volumen no corresponde al refactor en sí.
El desglose exacto por área es:

- **1.575 archivos / -225.417 líneas**: eliminación de
  `services/notification-service/node_modules/`, carpeta de dependencias de Node.js subida
  por error al repositorio en el commit `b0e120c` ("feat(notification-service): agregar
  microservicio de notificaciones multicanal", Jeremy Álvarez, 26/07/2026), antes de que
  existiera la regla `node_modules/` en `.gitignore` (añadida después, el 21/08/2026, commit
  `0357e95`).
- **75 archivos / +2.492/-904 líneas** en `services/svc-principal`: el refactor real de esta
  entrega — extracción de `presentation/application/domain/infrastructure`, puertos, y los 6
  patrones GoF documentados en el ADR-0005.
- **4 archivos / -989 líneas** en `docs/latex`: subproductos de compilación LaTeX
  (`main.aux`, `main.log`, `main.out`, `main.toc`), no contenido del manuscrito.
- **1 archivo / -3 líneas**: `results_fault_tolerance.csv` en la raíz.

## Decisión
Se documenta esta nota para separar, de forma auditable, los hechos que quedaron mezclados en
un único commit. No se reescribe el historial de Git para no comprometer la integridad de una
rama ya compartida por el equipo; esta nota deja constancia expresa de la separación de
autoría y alcance para cualquier auditoría posterior del historial.

## Consecuencias

**Positivas:**
- Trazabilidad explícita entre el volumen reportado por `git log --numstat` y el trabajo de
  diseño real, verificable contra los commits `a74791c` y `b0e120c`.
- Se confirma que, a la fecha de esta nota, cero archivos de `node_modules/` permanecen
  rastreados en `main` ni en `feature/entrega-4`.

**Negativas / lecciones:**
- El commit `a74791c` sigue sin ser atómico: mezclar una limpieza incidental con una decisión
  de diseño en el mismo commit dificultó la auditoría en el momento en que ocurrió. Los
  commits posteriores del equipo separan ambos tipos de cambio.
