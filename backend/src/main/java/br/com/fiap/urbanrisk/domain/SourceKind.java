package br.com.fiap.urbanrisk.domain;

/** Origem do dado e sua confiabilidade base (RN-01). */
public enum SourceKind {
    SENSOR(90, "Sensor IoT"),
    PUBLICA(75, "Base pública"),
    COMUNIDADE(40, "Registro comunitário");

    private final int baseConfidence;
    private final String label;

    SourceKind(int baseConfidence, String label) {
        this.baseConfidence = baseConfidence;
        this.label = label;
    }

    public int baseConfidence() {
        return baseConfidence;
    }

    public String label() {
        return label;
    }
}
