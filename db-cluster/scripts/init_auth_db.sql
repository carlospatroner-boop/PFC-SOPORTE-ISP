-- init_auth_db.sql
-- Esquema para auth-service (equipo ACC — Soporte Tecnico ISP)
-- Ejecutar con: cockroach sql --insecure --host=localhost:26257 -f init_auth_db.sql
--
-- Base de datos separada de ticket_db: cada microservicio es dueno de su propio esquema
-- (ver db-cluster/scripts/init_db.sql para el esquema de ticket-service).

CREATE DATABASE IF NOT EXISTS auth_db;
SET DATABASE = auth_db;

-- Usuarios del sistema. El id lo genera la aplicacion (Hibernate GenerationType.UUID,
-- ver domain/User.java) antes del INSERT; el DEFAULT de aqui es solo un resguardo para
-- filas insertadas fuera de la aplicacion (por ejemplo, un script de seed manual).
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         STRING UNIQUE NOT NULL,
    password_hash STRING NOT NULL,
    full_name     STRING NOT NULL,
    role          STRING NOT NULL,          -- CLIENTE | TECNICO | ADMIN (ver domain/Role.java)
    -- Solo se llena para TECNICO (ver AuthService.createUserAsAdmin). Debe usar
    -- exactamente los mismos valores que el enum Zone de ticket-service
    -- (QUEVEDO_CENTRO | QUEVEDO_NORTE | QUEVEDO_SUR) -- viaja como claim del JWT
    -- para que ticket-service filtre "tickets de mi zona" sin consultar otra base.
    zone          STRING,
    active        BOOL NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Refresh tokens: solo se persiste el hash SHA-256 del token crudo, nunca el valor
-- original (el valor crudo se devuelve una sola vez al cliente en la respuesta de
-- login/refresh). "revoked" + "replaced_by" sostienen la rotacion con deteccion de reuso:
-- si un token ya revocado se vuelve a presentar, se asume robo y se revocan todos los
-- tokens del usuario (ver service/AuthService.java).
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    token_hash   STRING UNIQUE NOT NULL,
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOL NOT NULL DEFAULT false,
    replaced_by  UUID NULL REFERENCES refresh_tokens(id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Verificacion rapida:
--   SELECT email, role, active FROM users;
--   SELECT id, user_id, revoked, replaced_by FROM refresh_tokens ORDER BY issued_at DESC;
