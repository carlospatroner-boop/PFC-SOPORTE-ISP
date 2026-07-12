package ec.edu.uteq.soporte.authservice.web;

import ec.edu.uteq.soporte.authservice.exception.DuplicateEmailException;
import ec.edu.uteq.soporte.authservice.exception.InvalidCredentialsException;
import ec.edu.uteq.soporte.authservice.exception.InvalidRequestException;
import ec.edu.uteq.soporte.authservice.exception.InvalidTokenException;
import ec.edu.uteq.soporte.authservice.exception.TokenReuseDetectedException;
import ec.edu.uteq.soporte.authservice.exception.UserNotFoundException;
import ec.edu.uteq.soporte.authservice.web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(null, ex.getMessage()));
    }

    @ExceptionHandler({DuplicateEmailException.class, InvalidRequestException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of(null, ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class, TokenReuseDetectedException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.of(null, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.of(null, "No tiene permisos para esta operacion"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Solicitud invalida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of(null, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(null, "Error interno: " + ex.getMessage()));
    }
}
