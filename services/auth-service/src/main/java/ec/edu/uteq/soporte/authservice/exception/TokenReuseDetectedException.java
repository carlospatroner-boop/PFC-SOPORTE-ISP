package ec.edu.uteq.soporte.authservice.exception;

/**
 * Se presento un refresh token que ya habia sido rotado/revocado previamente.
 * Esto es la senal clasica de robo de token (alguien capturo un refresh token viejo
 * y trato de usarlo despues de que el cliente legitimo ya roto a uno nuevo). Cuando
 * esto ocurre, AuthService revoca TODOS los refresh tokens del usuario afectado.
 */
public class TokenReuseDetectedException extends RuntimeException {
    public TokenReuseDetectedException(String message) {
        super(message);
    }
}
