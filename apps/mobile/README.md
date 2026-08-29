# Cliente móvil — técnicos de campo (Entrega 4, Módulo C)

App Android nativa (Kotlin + Jetpack Compose, MVVM) para técnicos de campo del equipo ACC:
ver tickets asignados, entrar al detalle y **cerrar en sitio** adjuntando una foto de evidencia
(cámara) y la ubicación del cierre (geolocalización).

## Requisitos

- JDK 17+ (probado con JDK 21).
- Android SDK con platform 34 y build-tools 34.0.0 (o el `sdkmanager` los instala solos si faltan).
- No hace falta instalar Gradle: el proyecto trae su propio wrapper (`gradlew`).

## Configurar el SDK local

Crear `local.properties` en esta carpeta (no se versiona) apuntando al SDK:

```properties
sdk.dir=C:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

## Compilar

```bash
./gradlew :app:assembleDebug
```

Genera `app/build/outputs/apk/debug/app-debug.apk`.

## Pruebas

```bash
./gradlew :app:testDebugUnitTest        # ViewModels, JUnit 5 + coroutines-test
./gradlew :app:connectedDebugAndroidTest # E2E instrumentada, requiere emulador/dispositivo
```

## Contra qué backend corre

Por defecto apunta a `10.0.2.2` (así ve el emulador de Android el `localhost` de la máquina
host) en los puertos reales de `auth-service` (8001) y `ticket-service` (8002) —
ver `AUTH_BASE_URL`/`TICKETS_BASE_URL` en `app/build.gradle.kts`. Para probar en un dispositivo
físico, cambiar esas URLs a la IP de LAN del host.

## Estado del módulo

- ✅ Login contra `/api/v1/auth/login`, JWT en `EncryptedSharedPreferences`.
- ✅ Listado de tickets asignados con caché offline (Room) y *pull-to-refresh*.
- ✅ Detalle de ticket + captura de foto (cámara) y ubicación (GPS) — las 2 capacidades exigidas.
- ⚠️ El cierre en sitio hoy solo actualiza el estado a `RESUELTO` (endpoint que ya existe). Falta
  que el backend acepte la foto y la ubicación del cierre — coordinar con el módulo de
  refactorización en capas antes de conectar ese último tramo.
- CI (`test-mobile`, `build-mobile-apk`) pendiente de integrar al pipeline general del proyecto.
