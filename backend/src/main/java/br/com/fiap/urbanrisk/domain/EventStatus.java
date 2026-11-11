package br.com.fiap.urbanrisk.domain;

/** Estados do ciclo de vida de um evento de risco. */
public enum EventStatus {
    REPORTADO, EM_VALIDACAO, CONFIRMADO, ALERTA_EMITIDO, EM_MONITORAMENTO, ENCERRADO, DESCARTADO;

    public boolean isOpen() {
        return this != ENCERRADO && this != DESCARTADO;
    }
}
