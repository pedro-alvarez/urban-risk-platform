package br.com.fiap.urbanrisk.domain;

/** Faixas de confiança (RN-02): Alta >= 80, Média 50-79, Baixa < 50. */
public enum ConfidenceLevel {
    ALTA, MEDIA, BAIXA;

    public static ConfidenceLevel of(int confidence) {
        if (confidence >= 80) {
            return ALTA;
        }
        return confidence >= 50 ? MEDIA : BAIXA;
    }
}
