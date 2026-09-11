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
- CI (`test-mobile`, `build-mobile-apk`) ya integrado al pipeline general (`.github/workflows/ci-cd.yml`).

## Paquete instalable (release/apk/)

El paquete firmado de esta entrega está en
[`release/apk/soporte-isp.apk`](../../release/apk/soporte-isp.apk), junto a su suma de
verificación SHA-256 en el mismo directorio (`release/apk/SHA256SUMS.txt`).

**Generación automática.** El job `build-mobile-apk` de `.github/workflows/ci-cd.yml` compila,
alinea (`zipalign`) y firma (`apksigner`) el APK de release en cada push, y lo publica como
artefacto descargable del propio *run* (`soporte-isp-release-firmado`, 90 días de retención) junto
con su suma recalculada — ya no es un paso manual. El `.jks` vive como secreto de GitHub Actions
en base64 (`ANDROID_KEYSTORE_BASE64`, con `ANDROID_KEYSTORE_PASSWORD` y `ANDROID_KEY_ALIAS`),
nunca en el repositorio.

**Instalar en un dispositivo o emulador con depuración USB habilitada:**

```bash
adb install release/apk/soporte-isp.apk
```

**Verificar antes de instalar** (que el archivo descargado sea exactamente el que el equipo
firmó, sin alteración):

```bash
cd release/apk
sha256sum -c SHA256SUMS.txt
```

**Verificar la firma del paquete en sí** (requiere `apksigner` de las *build-tools* del SDK de
Android):

```bash
apksigner verify --verbose release/apk/soporte-isp.apk
```

La clave de firma es autofirmada (uso académico, sin publicación en una tienda), generada con
`keytool` y validez de 10 000 días; el `.jks` en sí no se versiona porque el repositorio es
público y clonable sin credenciales — comprometer una clave de firma versionada en un
repositorio público es un riesgo real, no teórico.
