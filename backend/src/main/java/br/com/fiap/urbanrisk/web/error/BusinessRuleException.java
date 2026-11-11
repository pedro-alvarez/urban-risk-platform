package br.com.fiap.urbanrisk.web.error;

import org.springframework.http.HttpStatus;

/** Violação de regra de negócio (ex.: transição de estado inválida, alerta com confiança baixa). */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
