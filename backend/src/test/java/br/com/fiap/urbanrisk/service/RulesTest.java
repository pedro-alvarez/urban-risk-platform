package br.com.fiap.urbanrisk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.fiap.urbanrisk.domain.ConfidenceLevel;
import br.com.fiap.urbanrisk.domain.EventStatus;
import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.Evidence;
import br.com.fiap.urbanrisk.domain.RiskEvent;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.service.LifecycleRules.Action;
import br.com.fiap.urbanrisk.web.dto.Dtos.TrendView;

/** Testes unitários das regras de negócio (RN-01 a RN-05 e máquina de estados). */
class RulesTest {

    private final Instant t0 = Instant.parse("2026-11-12T10:00:00Z");
    private final RiskEvent holder = new RiskEvent(EventType.ALAGAMENTO, "Ribeira", true, RiskLevel.ALTO, "t", "d", t0);

    private Evidence ev(SourceKind kind, String name, int minutes, Double measurement) {
        return new Evidence(holder, kind, name, name, t0.plusSeconds(minutes * 60L), measurement);
    }

    // ---------------------------------------------------------------- RN-01 / RN-02
    @Test
    void confidenceUsesBaseOfPrincipalSourcePlus15PerIndependentSource() {
        ConfidenceCalculator c = new ConfidenceCalculator();
        assertEquals(40, c.calculate(List.of(ev(SourceKind.COMUNIDADE, "a", 0, null)), false));
        assertEquals(55, c.calculate(List.of(ev(SourceKind.COMUNIDADE, "a", 0, null), ev(SourceKind.PUBLICA, "b", 1, null)), false));
        assertEquals(90, c.calculate(List.of(ev(SourceKind.SENSOR, "s", 0, 50.0)), false));
    }

    @Test
    void confidenceIsCappedAt100AndRepeatedSameSourceDoesNotAddConfidence() {
        ConfidenceCalculator c = new ConfidenceCalculator();
        List<Evidence> same = List.of(ev(SourceKind.SENSOR, "s", 0, 1.0), ev(SourceKind.SENSOR, "s", 1, 2.0), ev(SourceKind.SENSOR, "s", 2, 3.0));
        assertEquals(90, c.calculate(same, false));
        List<Evidence> many = List.of(ev(SourceKind.SENSOR, "s", 0, 1.0), ev(SourceKind.PUBLICA, "p", 1, null), ev(SourceKind.COMUNIDADE, "c", 2, null));
        assertEquals(100, c.calculate(many, false));
    }

    @Test
    void staleSourceReducesConfidenceBy20() {
        ConfidenceCalculator c = new ConfidenceCalculator();
        assertEquals(70, c.calculate(List.of(ev(SourceKind.SENSOR, "s", 0, 1.0)), true));
    }

    @Test
    void confidenceLevelBands() {
        assertEquals(ConfidenceLevel.ALTA, ConfidenceLevel.of(80));
        assertEquals(ConfidenceLevel.MEDIA, ConfidenceLevel.of(79));
        assertEquals(ConfidenceLevel.MEDIA, ConfidenceLevel.of(50));
        assertEquals(ConfidenceLevel.BAIXA, ConfidenceLevel.of(49));
    }

    // ---------------------------------------------------------------- RN-03
    @Test
    void priorityFormula() {
        PriorityCalculator p = new PriorityCalculator();
        assertEquals(90, p.calculate(RiskLevel.ALTO, 100, true, false));   // 60 + 20 + 10
        assertEquals(95, p.calculate(RiskLevel.ALTO, 100, true, true));    // + tendência de alta
        assertEquals(30, p.calculate(RiskLevel.BAIXO, 50, false, false));  // 20 + 10
        assertTrue(p.calculate(RiskLevel.CRITICO, 90, false, false) > p.calculate(RiskLevel.ALTO, 100, true, false));
    }

    // ---------------------------------------------------------------- RN-05
    @Test
    void floodClassification() {
        RiskClassifier c = new RiskClassifier();
        assertEquals(RiskLevel.BAIXO, c.flood(65, null, false));
        assertEquals(RiskLevel.MEDIO, c.flood(80, null, false));
        assertEquals(RiskLevel.MEDIO, c.flood(100, null, false));
        assertEquals(RiskLevel.ALTO, c.flood(101, null, false));
        assertEquals(RiskLevel.CRITICO, c.flood(121, null, false));
        assertEquals(RiskLevel.ALTO, c.flood(90, false, true));      // drenagem inoperante em área vulnerável: +1
        assertEquals(RiskLevel.MEDIO, c.flood(90, false, false));    // fora de área vulnerável: sem agravante
        assertEquals(RiskLevel.BAIXO, c.flood(70, false, true));     // nível ainda baixo: sem agravante
    }

    @Test
    void heatClassification() {
        RiskClassifier c = new RiskClassifier();
        assertEquals(RiskLevel.BAIXO, c.heat(31, null, false));
        assertEquals(RiskLevel.MEDIO, c.heat(35, null, false));
        assertEquals(RiskLevel.ALTO, c.heat(41, true, true));
        assertEquals(RiskLevel.CRITICO, c.heat(41, false, true));    // sem abrigo em área vulnerável: +1
        assertEquals(RiskLevel.CRITICO, c.heat(46, null, false));
    }

    @Test
    void servicesClassificationFollowsFarolRules() {
        RiskClassifier c = new RiskClassifier();
        assertEquals(RiskLevel.CRITICO, c.services(false, false, true, false));   // energia e água fora
        assertEquals(RiskLevel.ALTO, c.services(true, true, false, true));        // socorro sem comunicação
        assertEquals(RiskLevel.MEDIO, c.services(false, true, true, false));      // só energia
        assertEquals(RiskLevel.MEDIO, c.services(true, true, false, false));      // comunicação fora, sem socorro
        assertEquals(RiskLevel.BAIXO, c.services(true, true, true, false));
    }

    // ---------------------------------------------------------------- máquina de estados
    @Test
    void lifecycleAllowsOnlyDocumentedTransitions() {
        LifecycleRules r = new LifecycleRules();
        assertEquals(Optional.of(EventStatus.EM_VALIDACAO), r.next(EventStatus.REPORTADO, Action.VALIDAR));
        assertEquals(Optional.of(EventStatus.CONFIRMADO), r.next(EventStatus.EM_VALIDACAO, Action.CONFIRMAR));
        assertEquals(Optional.of(EventStatus.ALERTA_EMITIDO), r.next(EventStatus.CONFIRMADO, Action.ALERTAR));
        assertEquals(Optional.of(EventStatus.EM_MONITORAMENTO), r.next(EventStatus.ALERTA_EMITIDO, Action.MONITORAR));
        assertEquals(Optional.of(EventStatus.ALERTA_EMITIDO), r.next(EventStatus.EM_MONITORAMENTO, Action.ALERTAR));
        assertEquals(Optional.of(EventStatus.ENCERRADO), r.next(EventStatus.EM_MONITORAMENTO, Action.ENCERRAR));
        // proibidas
        assertTrue(r.next(EventStatus.REPORTADO, Action.ALERTAR).isEmpty());
        assertTrue(r.next(EventStatus.CONFIRMADO, Action.ENCERRAR).isEmpty());
        assertTrue(r.next(EventStatus.ENCERRADO, Action.VALIDAR).isEmpty());
        assertTrue(r.next(EventStatus.DESCARTADO, Action.CONFIRMAR).isEmpty());
        assertFalse(EventStatus.DESCARTADO.isOpen());
    }

    // ---------------------------------------------------------------- análise de tendência
    @Test
    void trendDetectsRisingLevelAndProjectsTimeToLimit() {
        TrendAnalyzer a = new TrendAnalyzer();
        List<Evidence> rising = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rising.add(ev(SourceKind.SENSOR, "s", i, 60.0 + i * 4));    // +4 pontos por minuto
        }
        TrendView t = a.analyze(rising, EventType.ALAGAMENTO);
        assertEquals(TrendAnalyzer.RISING, t.direction());
        assertEquals(4.0, t.slopePerMinute(), 0.01);
        assertNotNull(t.minutesToLimit());
        assertEquals(6.0, t.minutesToLimit(), 0.1);     // (100 - 76) / 4
    }

    @Test
    void trendIsStableWithFewPointsOrFlatSeries() {
        TrendAnalyzer a = new TrendAnalyzer();
        assertEquals(TrendAnalyzer.STABLE, a.analyze(List.of(ev(SourceKind.SENSOR, "s", 0, 50.0)), EventType.ALAGAMENTO).direction());
        List<Evidence> flat = List.of(ev(SourceKind.SENSOR, "s", 0, 50.0), ev(SourceKind.SENSOR, "s", 1, 50.0), ev(SourceKind.SENSOR, "s", 2, 50.0));
        TrendView t = a.analyze(flat, EventType.ALAGAMENTO);
        assertEquals(TrendAnalyzer.STABLE, t.direction());
        assertNull(t.minutesToLimit());
    }

    @Test
    void trendDetectsFallingLevel() {
        TrendAnalyzer a = new TrendAnalyzer();
        List<Evidence> falling = List.of(ev(SourceKind.SENSOR, "s", 0, 90.0), ev(SourceKind.SENSOR, "s", 1, 85.0),
                ev(SourceKind.SENSOR, "s", 2, 80.0), ev(SourceKind.SENSOR, "s", 3, 75.0));
        assertEquals(TrendAnalyzer.FALLING, a.analyze(falling, EventType.ALAGAMENTO).direction());
    }
}
