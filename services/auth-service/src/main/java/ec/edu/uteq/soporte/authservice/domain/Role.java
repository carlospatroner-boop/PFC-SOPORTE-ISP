package ec.edu.uteq.soporte.authservice.domain;

/**
 * Roles predefinidos del sistema de soporte tecnico (equipo ACC). El registro publico
 * (`POST /api/v1/auth/register`) siempre asigna CLIENTE; TECNICO y ADMIN solo se crean
 * a traves del endpoint administrativo (`POST /api/v1/auth/admin/users`, protegido por
 * rol ADMIN) — ver service/AuthService.java.
 */
public enum Role {
    CLIENTE,
    TECNICO,
    ADMIN
}
