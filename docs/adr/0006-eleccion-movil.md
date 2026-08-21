# ADR-0006: Elección de framework para la aplicación móvil (Android nativo/Kotlin)

## Estado
Aceptado — Entrega 4 (Agosto 2026)

## Contexto
El Módulo C de la guía de Entrega 4 permite dos rutas para el cliente móvil obligatorio:
Android nativo (Kotlin + Jetpack Compose) o multiplataforma (Flutter 3.x o React Native
0.74+). El anexo de preguntas obligatorias para la defensa (Anexo B, pregunta 1) exige
justificar esa elección con al menos dos criterios **cuantitativos**, no solo preferencia
estética — es una decisión con consecuencias medibles en rendimiento, mantenibilidad y
*time-to-market* [12], [13], [14].

El cliente móvil de ACC/Soporte-ISP es para técnicos de campo: debe usar cámara (evidencia
del cierre de un ticket) y geolocalización (`FusedLocationProviderClient`) de forma continua
durante la jornada, con la app corriendo en dispositivos de gama variable que la operadora
asigna al personal técnico — no en los teléfonos de los propios técnicos.

## Decisión
Se elige **Android nativo con Kotlin + Jetpack Compose** (MVVM, DI manual). Los criterios
cuantitativos que sustentan la elección, medidos directamente sobre el proyecto ya construido:

1. **Tamaño del artefacto instalable.** El APK de depuración actual (`app-debug.apk`,
   23 archivos Kotlin, 1508 líneas) pesa **11.06 MB**, sin ningún motor de renderizado
   embebido adicional: Jetpack Compose compila a las mismas vistas nativas de Android (bytecode
   ART), a diferencia de un framework multiplataforma que empaqueta su propio motor
   (Skia/Flutter engine, o el puente JavaScript de React Native) dentro de cada instalación,
   además del código de la app. Para un cliente de campo que se instala/actualiza sobre
   conexiones móviles potencialmente limitadas (zonas rurales del área de cobertura), un
   artefacto más liviano es una ventaja medible, no cosmética.
2. **Dependencias de terceros para acceder a las capacidades del dispositivo exigidas.** El
   proyecto declara 20 dependencias `implementation` en total. Cámara y geolocalización —las
   dos capacidades obligatorias del Módulo C— se resuelven con **cero dependencias de
   interoperabilidad**: la cámara usa `ActivityResultContracts.TakePicture` (parte del propio
   SDK de Android, sin librería adicional) y la geolocalización usa una única dependencia
   oficial de Google (`play-services-location`). Un framework multiplataforma necesitaría, en
   cambio, un *plugin* puente (`camera`, `geolocator`, canales de método/FFI) para llegar a esas
   mismas APIs nativas, con la sobrecarga de serialización y el riesgo de mantenimiento de un
   paquete de terceros que eso implica.

Como criterio adicional (cualitativo, de contexto): el backend del PFC ya es JVM (Java 21 +
Spring Boot desde la E2), por lo que Kotlin comparte máquina virtual, herramientas de build
(Gradle) y convenciones con el resto del equipo — el salto conceptual es menor que adoptar Dart
(Flutter) o JavaScript/TypeScript con un puente nativo (React Native) desde cero.

## Consecuencias

**Positivas:**
- Acceso directo y de primera clase a `CameraX`/`ActivityResultContracts` y
  `FusedLocationProviderClient`, sin capa de interoperabilidad que traducir ni depurar.
- Artefacto instalable más liviano, medido y reproducible (`./gradlew assembleDebug`).
- Un solo lenguaje/VM (JVM) entre buena parte del backend y el cliente móvil reduce la curva de
  aprendizaje del equipo, que no tenía experiencia previa con Dart ni con los puentes nativos de
  React Native.

**Negativas / riesgos:**
- Sin código compartido entre plataformas: si el PFC exigiera también un cliente iOS, este
  trabajo no es reutilizable (no aplica en este PFC, pero es una limitación real de la decisión
  frente al ideal multiplataforma).
- El equipo debe mantener un stack de build adicional (Gradle/Android) separado del de la web
  (Vite/npm) y del backend (Maven), en vez de compartir un único toolchain como ofrecería
  React Native/TypeScript.
- La comparación cuantitativa se hizo contra las cifras conocidas y documentadas de los
  frameworks multiplataforma en la literatura citada [12], [13], [14], no contra una
  implementación equivalente propia construida en Flutter/RN para este mismo PFC — no había
  presupuesto de tiempo del equipo para construir ambas versiones solo para comparar.
