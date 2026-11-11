package br.com.fiap.urbanrisk.domain;

/** Nível de risco de um evento. O peso entra no cálculo de prioridade (RN-03). */
public enum RiskLevel {
    BAIXO(1), MEDIO(2), ALTO(3), CRITICO(4);

    private final int weight;

    RiskLevel(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean atLeast(RiskLevel other) {
        return weight >= other.weight;
    }

    /** Próximo nível acima (CRITICO é o teto). */
    public RiskLevel up() {
        return this == CRITICO ? CRITICO : values()[ordinal() + 1];
    }

    public static RiskLevel max(RiskLevel a, RiskLevel b) {
        return a.weight >= b.weight ? a : b;
    }
}
