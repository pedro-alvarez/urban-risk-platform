package br.com.fiap.urbanrisk.service;

import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.RiskLevel;

/**
 * RN-03: prioridade = peso do risco x 20 + confiança x 0,2 + 10 se área vulnerável.
 * Extensão de análise: +5 quando a tendência recente das medições é de alta (ver {@link TrendAnalyzer}).
 */
@Component
public class PriorityCalculator {

    public int calculate(RiskLevel risk, int confidence, boolean vulnerableArea, boolean risingTrend) {
        double value = risk.weight() * 20 + confidence * 0.2 + (vulnerableArea ? 10 : 0) + (risingTrend ? 5 : 0);
        return (int) Math.round(value);
    }
}
