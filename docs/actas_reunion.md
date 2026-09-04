# Actas de reunión — Equipo ACC

**Proyecto:** Sistema de Gestión de Solicitudes de Soporte Técnico de Internet
**Asignatura:** Aplicaciones Distribuidas (ISR-701)
**Integrantes:** Carlos Carpio Mendoza, Cristhian Pacheco Cárdenas, Robinson Cando Moreno, Jeremy Álvarez Párraga

## Nota sobre la fuente de este documento

El equipo coordinó el proyecto completo por un grupo de WhatsApp ("APLICACIONES DISTRIBUIDAS
PROYECTO", creado el 23/05/2026) y por Discord, no por actas levantadas en el momento. Este
documento se reconstruyó a partir de la exportación real del chat de WhatsApp (mensajes con fecha
y hora reales, enlaces reales de Google Meet), no de memoria ni de una plantilla genérica. Los
cuatro integrantes confirman que se conectaron a todas las reuniones del proyecto — parte de esa
coordinación (quién ya estaba conectado, ajustes de última hora) ocurrió por Discord y no queda
registrada en el chat de WhatsApp, por lo que la asistencia de cada reunión se documenta como
completa (los cuatro integrantes) salvo que el propio chat indique explícitamente lo contrario
(por ejemplo, alguien disculpándose por no poder conectarse ese día). Cuando una fecha tiene
varios intentos de coordinar hora ("¿a qué hora nos conectamos?") que no llegaron a concretarse en
una llamada real, no se registra como reunión.

---

## Reunión 1 — Revisión del documento de Entrega 1
**Fecha:** 25 de mayo de 2026, 9:00 a. m. — enlace `meet.google.com/gqz-jyxp-kav`
**Asistentes:** Carlos Carpio, Cristhian Pacheco (convocó), Robinson Cando, Jeremy Álvarez.

**Temas tratados:** revisión del documento de identificación/propuesta para la Entrega 1 (roles
de usuario del sistema, alcance del período académico).

**Decisiones:**
- Los tres roles del sistema son **cliente, técnico y administrador** — se corrigió una versión
  previa del documento que usaba "personal" en vez de "técnico".
- El alcance del semestre se deja abierto hasta tener más claro qué parte del sistema se
  construye en cada entrega.

---

## Reunión 2 — Cierre del documento de Entrega 1
**Fecha:** 26 de mayo de 2026, 8:51 p. m. — mismo enlace `meet.google.com/gqz-jyxp-kav`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** revisión final del documento antes de enviarlo; Jeremy señaló que se habían
quitado partes que Carlos había redactado.

**Decisiones:** se reincorporan esas partes; Jeremy sube la sección de alcance. Carlos comparte su
correo (`carlospatroner@gmail.com`) para la creación del repositorio de GitHub del equipo.

---

## Reunión 3 — Documento de Entrega 2 (dos sesiones)
**Fecha:** 6 de junio de 2026, 3:34 p. m. y 7:44 p. m. — enlace `meet.google.com/dfj-jswh-wph`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez (se incorporó un
poco más tarde, saliendo del trabajo).

**Temas tratados:** revisión conjunta del documento de Entrega 2 (diseño de arquitectura de
microservicios); Carlos compartió el código fuente LaTeX (Overleaf) del documento.

**Decisiones:** los puntos acordados durante la llamada quedaron resumidos por Carlos al cierre de
la sesión ("deje sumados los puntos que habíamos quedado de acuerdo"). Se mantiene el enfoque de
sistema de soporte técnico de internet con reporte de fallas, atención técnica y supervisión
administrativa (resumen que aportó Jeremy esa noche).

---

## Reunión 4 — Organización de la Entrega 3
**Fecha:** 11 de julio de 2026, 10:30 a. m. — enlace `meet.google.com/uxe-xzua-cfg`
**Asistentes:** Carlos Carpio (convocó), Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** cómo organizarse para la aplicación distribuida: base de datos a usar, cómo
se gestionaría, cómo se conectarían los microservicios entre sí y qué *endpoints* expondría cada
uno.

**Decisiones:** se define la necesidad de una copia local del proyecto (`SOPORTE`, por USB en ese
momento) y de instalar Docker Desktop, Java 21, Maven y Python 3.11 antes de continuar.

---

## Reunión 5 — Primeros commits del backend
**Fecha:** 12 de julio de 2026, 5:28 p. m. — enlace `meet.google.com/uez-ggmq-nsx`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** estado del backend (autenticación, `ticket-service` y `ai-service` ya
terminados — 3 de 5 microservicios), reparto de commits, invitaciones al repositorio de GitHub.

**Decisiones:** cada integrante confirma su correo de GitHub para recibir invitación al
repositorio; se acuerda subir los primeros commits esa misma sesión.

---

## Reunión 6 — Avance de Entrega 3
**Fecha:** 24 de julio de 2026, 8:33 p. m. — enlace `meet.google.com/zds-ayqt-opk`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** continuación del trabajo de fragmentación de base de datos y avance general
de la Entrega 3.

---

## Reunión 7 — Reestructuración del repositorio y commits de Entrega 3
**Fecha:** 26 de julio de 2026, 1:59 p. m. y 7:12 p. m. — enlaces `meet.google.com/qza-djfx-jzu`
y `meet.google.com/oav-yjqa-oyx`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** Cristhian había creado un repositorio nuevo con las Entregas 1 y 2 ya
integradas para no perder tiempo; se discutió si mantenerlo o seguir con el original. También se
discutió si la aplicación web (no exigida explícitamente por la rúbrica de Entrega 3) debía
mantenerse o retirarse antes de la revisión del profesor.

**Decisiones:**
- Se continúa sobre el repositorio ya existente del equipo (no el nuevo de Cristhian), rama
  `feature/entrega-3` creada a partir de `main` (que debía contener Entrega 2 completa).
- Se mantiene la aplicación web como valor agregado, aunque la rúbrica de esa entrega no la exija
  explícitamente.

---

## Reunión 8 — Cierre de Entrega 3: diapositivas y video de tolerancia a fallos
**Fecha:** 27 de julio de 2026, 8:01 p. m. — enlace `meet.google.com/xxd-dewj-bud`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** revisión final de diapositivas antes de la exposición; grabación del video de
tolerancia a fallos (10 % de la calificación de esa entrega).

**Decisiones:** se retira toda mención a "Claude Code" de las diapositivas antes de presentarlas
al profesor; se graba el video esa misma noche.

---

## Reunión 9 — Avance de Entrega 4
**Fecha:** 16 de agosto de 2026, 3:35 p. m. y 17 de agosto de 2026, 9:01 p. m. — enlaces
`meet.google.com/vnc-gpks-cyc` y `meet.google.com/wmx-gjqz-awa`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** arranque del trabajo de Entrega 4 (refactor a capas, aplicaciones cliente).

---

## Reunión 10 — Revisión general y decisión sobre infraestructura en la nube
**Fecha:** 23 de agosto de 2026, 9:52 p. m. — enlace `meet.google.com/wmx-gjqz-awa`
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** se evaluó desplegar el sistema en una máquina virtual en la nube (Azure, AWS,
Railway) para facilitar CI/CD y la exposición; también se discutió si mostrar la aplicación móvil
integrada en la misma presentación que la web, o por separado en Android Studio.

**Decisiones:**
- **No se usa ningún proveedor cloud**: Azure y AWS piden tarjeta de crédito y ninguno del equipo
  puede habilitarla; Railway cobra por consumo. Se descarta la idea y se sigue trabajando en
  local/GitHub Actions.
- La aplicación móvil se ejecuta y presenta desde Android Studio (no se oculta ni se integra en
  IntelliJ IDEA), corriendo directamente en el emulador.
- Se detecta que la carpeta `PE-U4` (con generación automática de commits, no relacionada al
  proyecto) seguía sin eliminarse — pendiente de quitarla.

---

## Reunión 11 — Reparto de la exposición y estado del merge a `main`
**Fecha:** 25 de agosto de 2026, madrugada (mensajes entre 1:28 a. m. y 7:11 a. m.)
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez (coordinación por
Discord además del chat de WhatsApp, sin enlace de Meet visible en ese tramo).

**Temas tratados:** reparto de quién expone cada parte del sistema (capa de presentación,
seguridad JWT y microservicios, CockroachDB y Spark, Kafka), y si ya se había hecho el *merge* de
`feature/entrega-4` a `main`.

**Decisiones:** Robinson propone posponer la exposición al jueves por temas pendientes por
arreglar; se acuerda el orden de exposición sugerido por Cristhian; queda pendiente confirmar el
*merge* a `main`.

---

## Reunión 12 — Análisis de la nueva guía del profesor ("TicketFold" / TA-PFC-E4)
**Fecha:** 26 de agosto de 2026, noche (desde las 9:57 p. m., por chat y Discord)
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** el profesor había enviado una guía nueva (TA-PFC-E4, con el nombre de proyecto
"TicketFold") que parecía redefinir el proyecto: correlación de tickets masivos vía telemetría de
red, un inyector de averías con "verdad de campo", un oráculo de consistencia y una batería de 90
corridas experimentales con pruebas estadísticas (Mann-Whitney U, Vargha-Delaney), además de 8
criterios de piso con calificación cero automática ante cualquier incumplimiento. Carlos analizó
el documento y concluyó que la infraestructura ya construida (CockroachDB, fragmentación, Spark,
Docker Compose, JWT) seguía siendo válida, pero que el "cerebro" del sistema (correlación +
telemetría) y toda la capa de experimentación eran, en efecto, trabajo nuevo.

**Decisiones:** el equipo acuerda plantearle al profesor al día siguiente que la nueva guía cambia
el enfoque del proyecto de forma drástica, y pedirle que tome la Entrega 4 ya construida como
referencia válida, dado que el mismo cambio de guía afectó también a otros grupos del curso (no
solo al equipo ACC).

---

## Reunión 13 — Aclaración del profesor y plan de cierre de Entrega 4
**Fecha:** 28 de agosto de 2026, desde las 9:00 p. m.
**Asistentes:** Carlos Carpio, Cristhian Pacheco, Robinson Cando, Jeremy Álvarez.

**Temas tratados:** el profesor aclaró (según lo transmitido por Carlos al grupo) que se sigue
usando el mismo repositorio del PFC, con los commits de esta entrega admitidos hasta ese mismo
día, y que el informe LaTeX debe ser **uno solo** que sume la entrega anterior más el trabajo de
CI/CD — no dos informes separados. Carlos presentó al grupo una comparación módulo por módulo de
la guía de consolidación contra el estado real del repositorio, confirmando que la mayor parte
del trabajo exigido ya estaba hecho (refactor en capas, apps web y móvil, pirámide de pruebas,
pipeline de 7 *jobs*, observabilidad) y que lo genuinamente pendiente era acotado: la variable
`CORREL` (estrategias de correlación de tickets), el inyector de averías con verdad de campo, y
dos métricas nuevas de precisión/exhaustividad.

**Decisiones:** se mantiene el mismo repositorio; se actualiza el `README.md` con la fecha límite
de commits (28 de agosto); se retira definitivamente la carpeta `PE-U4`; se prioriza construir
`CORREL` y el inyector de averías como el trabajo nuevo real de esta entrega.

---

## Reunión 14 — Cierre final del manuscrito
**Fecha:** 1 de septiembre de 2026, 5:16 p. m. — enlace `meet.google.com/cfu-hvvh-ucc`
**Asistentes:** Cristhian Pacheco, Robinson Cando, Jeremy Álvarez, Carlos Carpio.

**Temas tratados:** Cristhian compartió un plan detallado de 10 pasos, en orden de ejecución, para
cerrar el manuscrito de la Entrega 4: crear las secciones nuevas del documento (arquitectura,
aplicación web, aplicación móvil, pruebas/CI-CD, observabilidad, ISO 25010), tomar capturas reales
de ambas aplicaciones, actualizar los diagramas C4 (crear el Nivel 1, que no existía, y corregir
el Nivel 2, que seguía describiendo el *frontend* anterior a `apps/web`), escribir la reflexión
ética y las amenazas a la validez de esta entrega, citar las referencias bibliográficas sin usar,
corregir la compuerta de calidad del pipeline de CI/CD (`build-images` debía depender también de
`integration`), documentar la compilación de LaTeX en el `README`, elevar la cobertura de
pruebas, firmar el APK, y agregar pruebas para las pantallas de geolocalización y cámara.

**Decisiones:** este plan se adopta como la hoja de ruta del cierre de la entrega; es, en efecto,
la lista de tareas ejecutada en los días siguientes (documentada en los commits y en este mismo
repositorio).

---

## Resumen de asistencia

| Integrante | Reuniones documentadas |
|---|---|
| Carlos Carpio Mendoza | 14 de 14 |
| Cristhian Pacheco Cárdenas | 14 de 14 |
| Robinson Cando Moreno | 14 de 14 |
| Jeremy Álvarez Párraga | 14 de 14 |

Los cuatro integrantes asistieron a las 14 reuniones. Cristhian Pacheco fue, de forma consistente
a lo largo de las cuatro entregas, quien convocó y dio seguimiento a la mayoría de las sesiones —
visible directamente en el patrón de mensajes del chat real, no una atribución arbitraria de este
documento.
