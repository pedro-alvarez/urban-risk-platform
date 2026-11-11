package br.com.fiap.urbanrisk.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.Evidence;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.web.dto.Dtos.TrendView;

/**
 * Análise de tendência que apoia a priorização dos alertas.
 *
 * Ajusta uma reta (mínimos quadrados) às últimas medições de sensores do evento e estima a taxa de variação
 * por minuto. Para alagamento, projeta em quantos minutos o nível chegará ao limite de atenção (100%).
 * Uma tendência de alta aumenta a prioridade do evento (ver {@link PriorityCalculator}).
 */
@Component
public class TrendAnalyzer {

    public static final String RISING = "SUBINDO";
    public static final String FALLING = "CAINDO";
    public static final String STABLE = "ESTAVEL";

    private static final int WINDOW = 6;
    private static final double THRESHOLD_PER_MINUTE = 0.3;

    public TrendView analyze(List<Evidence> evidences, EventType type) {
        List<Evidence> points = evidences.stream()
                .filter(e -> e.getMeasurement() != null && e.getKind() == SourceKind.SENSOR)
                .sorted(Comparator.comparing(Evidence::getReceivedAt))
                .toList();
        if (points.size() > WINDOW) {
            points = points.subList(points.size() - WINDOW, points.size());
        }
        if (points.size() < 3) {
            return new TrendView(STABLE, 0, null);
        }
        double t0 = points.get(0).getReceivedAt().toEpochMilli();
        int n = points.size();
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (Evidence e : points) {
            double x = (e.getReceivedAt().toEpochMilli() - t0) / 60000.0;   // minutos
            double y = e.getMeasurement();
            sx += x;
            sy += y;
            sxy += x * y;
            sxx += x * x;
        }
        double denominator = n * sxx - sx * sx;
        double slope = Math.abs(denominator) < 1e-9 ? 0 : (n * sxy - sx * sy) / denominator;
        String direction = slope > THRESHOLD_PER_MINUTE ? RISING : (slope < -THRESHOLD_PER_MINUTE ? FALLING : STABLE);

        Double minutesToLimit = null;
        double last = points.get(points.size() - 1).getMeasurement();
        if (type == EventType.ALAGAMENTO && slope > THRESHOLD_PER_MINUTE && last < 100) {
            minutesToLimit = Math.round((100 - last) / slope * 10) / 10.0;
        }
        return new TrendView(direction, Math.round(slope * 100) / 100.0, minutesToLimit);
    }
}
