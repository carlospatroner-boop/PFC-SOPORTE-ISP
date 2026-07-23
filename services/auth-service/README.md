# auth-service

Microservicio de autenticacion del PFC (equipo ACC — Soporte Tecnico ISP). Java 21 + Spring
Boot 3.2.5, conectado por JDBC a su propia base de datos (`auth_db`) en el mismo cluster
CockroachDB del proyecto (carpeta `db-cluster/` en la raiz del repo).

## Responsabilidades

- Registrar nuevos usuarios con roles predefinidos (`CLIENTE`, `TECNICO`, `ADMIN`).
- Autenticar credenciales y emitir tokens JWT con claims de rol y permisos.
- Revocar sesiones y gestionar la rotacion de refresh tokens (con deteccion de reuso).
- Exponer un endpoint de validacion de token (`GET /validate`) pensado para ser consumido por
  el futuro API Gateway.

## Cómo correrlo

### 1. Cargar el esquema

Con el cluster de `db-cluster/` ya levantado (ver `../../db-cluster/README.md`):

```bash
cockroach sql --insecure --host=localhost:26257 -f ../../db-cluster/scripts/init_auth_db.sql
```

### 2a. Localmente (VS Code / mvn)

```bash
mvn spring-boot:run
```

El servicio queda en `http://localhost:8001`. Al primer arranque, si no existe ninguna cuenta
ADMIN todavia, se crea una automaticamente (ver `config/AdminBootstrap.java`):
`admin@soporte.local` / `Admin123!` por defecto — **cambiar esta contrasena** (o sobreescribir
`ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD`) en cualquier entorno que no sea tu propia
maquina de desarrollo.

### 2b. Con Docker (uniendose al cluster por la red `soporte-net`)

```bash
docker network inspect soporte-net >/dev/null 2>&1 || docker network create soporte-net
docker build -t auth-service .
docker run -d --name auth-service --network soporte-net \
  -e DB_URL=jdbc:postgresql://roach1:26257/auth_db?sslmode=disable \
  -e AUTH_JWT_SECRET=<generar-un-secreto-real-de-al-menos-32-bytes> \
  -p 8001:8001 auth-service
```

## Endpoints

| Metodo | Ruta | Auth | Descripcion |
|---|---|---|---|
| POST | `/api/v1/auth/register` | publico | Registro de auto-servicio; siempre crea rol `CLIENTE` |
| POST | `/api/v1/auth/login` | publico | Devuelve `accessToken` + `refreshToken` |
| POST | `/api/v1/auth/refresh` | publico (token en el body) | Rota el refresh token |
| POST | `/api/v1/auth/logout` | publico (token en el body) | Revoca un refresh token |
| GET | `/api/v1/auth/validate` | Bearer access token | Valida el token; lo consume el API Gateway |
| POST | `/api/v1/auth/admin/users` | Bearer access token, rol ADMIN | Crea usuarios con cualquier rol (CLIENTE/TECNICO/ADMIN) |

Ejemplo de flujo completo:

```bash
# Registro (siempre CLIENTE)
curl -s -X POST localhost:8001/api/v1/auth/register -H "Content-Type: application/json" \
  -d '{"email":"cliente@test.com","password":"Passw0rd!","fullName":"Cliente Test"}'

# Login
curl -s -X POST localhost:8001/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"cliente@test.com","password":"Passw0rd!"}'

# Validar (usar el accessToken devuelto arriba)
curl -s localhost:8001/api/v1/auth/validate -H "Authorization: Bearer <accessToken>"

# Renovar (usar el refreshToken devuelto en login)
curl -s -X POST localhost:8001/api/v1/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# Cerrar sesion
curl -s -X POST localhost:8001/api/v1/auth/logout -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Deteccion de reuso de refresh tokens

Cada `refresh` rota el token: revoca la fila vieja y emite una pareja nueva. Si alguien vuelve
a presentar un refresh token que ya fue rotado/revocado antes (por ejemplo, porque fue
interceptado), `AuthService.refresh(...)` lo interpreta como una senal de robo y revoca
**todos** los refresh tokens activos de ese usuario, no solo el presentado. Verificable con:

```sql
SELECT id, revoked, replaced_by FROM auth_db.refresh_tokens WHERE user_id = '<uuid>';
```

## Limitacion conocida

Este servicio emite y valida tokens, pero **nada los exige todavia en la frontera de
ticket-service** — no existe un API Gateway en este repo que intercepte las llamadas a
`ticket-service` y llame a `/api/v1/auth/validate` antes de dejarlas pasar (ver la misma
limitacion documentada en `services/svc-principal/README.md`). Conectar ambos servicios es
trabajo futuro, fuera del alcance de esta entrega.

## Tests

```bash
mvn test
```

Pruebas unitarias puras (Mockito + JUnit 5 + AssertJ, sin necesidad del cluster levantado):
`AuthServiceTest` (registro, login, rotacion y deteccion de reuso de refresh tokens) y
`JwtServiceTest` (ida y vuelta de claims, rechazo de tokens expirados).
