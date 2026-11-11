package br.com.fiap.urbanrisk.service;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.Evidence;

/**
 * RN-01: confiança = base da fonte principal + 15 pontos por evidência independente adicional (máx. 100).
 * RN-06: se a fonte principal está sem atualização (offline), a confiança cai 20 pontos.
 * "Independente" significa vinda de uma fonte (sourceName) diferente das já consideradas.
 */
@Component
public class ConfidenceCalculator {

    public static final int EXTRA_PER_INDEPENDENT_SOURCE = 15;
    public static final int STALE_PENALTY = 20;

    public int calculate(List<Evidence> evidences, boolean principalSourceStale) {
        if (evidences.isEmpty()) {
            return 0;
        }
        int base = evidences.get(0).getKind().baseConfidence();
        long independentSources = evidences.stream().map(Evidence::getSourceName).distinct().count();
        int extra = (int) (independentSources - 1) * EXTRA_PER_INDEPENDENT_SOURCE;
        int confidence = Math.min(100, base + extra);
        if (principalSourceStale) {
            confidence = Math.max(0, confidence - STALE_PENALTY);
        }
        return confidence;
    }
}
