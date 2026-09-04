package ec.edu.uteq.soporte.reportservice.application;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
