package ec.edu.uteq.soporte.authservice.application;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
