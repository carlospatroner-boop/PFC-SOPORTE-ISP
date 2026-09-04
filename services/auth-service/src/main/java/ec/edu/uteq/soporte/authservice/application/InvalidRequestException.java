package ec.edu.uteq.soporte.authservice.application;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
