package br.com.fiap.urbanrisk.web.error;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.fiap.urbanrisk.web.dto.Dtos.ErrorView;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorView> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ErrorView("VALIDATION", "Dados inválidos", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorView> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorView("BAD_REQUEST", "Corpo da requisição ilegível ou com valor inválido", List.of()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorView> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorView("NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorView> business(BusinessRuleException ex) {
        return ResponseEntity.status(ex.status()).body(new ErrorView("BUSINESS_RULE", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorView> illegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorView("BAD_REQUEST", ex.getMessage(), List.of()));
    }
}
