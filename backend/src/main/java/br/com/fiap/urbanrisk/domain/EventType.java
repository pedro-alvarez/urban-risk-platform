package br.com.fiap.urbanrisk.domain;

/** Tipos de risco urbano monitorados pela plataforma. */
public enum EventType {
    ALAGAMENTO("Alagamento"),
    CALOR("Ilha de calor"),
    SERVICOS("Interrupção de serviço");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
