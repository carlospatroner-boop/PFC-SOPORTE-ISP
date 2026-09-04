package ec.edu.uteq.soporte.authservice.application;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
