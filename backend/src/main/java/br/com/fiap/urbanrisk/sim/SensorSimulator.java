package br.com.fiap.urbanrisk.sim;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.service.EventService;
import br.com.fiap.urbanrisk.web.dto.Dtos.ReadingRequest;

/**
 * Gera leituras de sensores simuladas em intervalos regulares, para demonstrar atualização em tempo real.
 * O sensor S-021 (Porto Seco) sobe de nível gradualmente e depois recua; a estação EM-05 (Jardim das Flores) oscila.
 * Desative com urbanrisk.simulator.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "urbanrisk.simulator.enabled", havingValue = "true")
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private final EventService events;
    private final Random random = new Random(42);
    private double floodLevel = 55;       // % do limite de atenção (Porto Seco)
    private int floodDirection = 1;
    private double heat = 34;             // índice de calor em °C (Jardim das Flores)
    private int tick = 0;

    public SensorSimulator(EventService events) {
        this.events = events;
    }

    @Scheduled(initialDelay = 8000, fixedDelayString = "${urbanrisk.simulator.interval-ms:5000}")
    public void tick() {
        try {
            tick++;
            floodLevel += floodDirection * (2 + random.nextDouble() * 4);
            if (floodLevel > 135) {
                floodDirection = -1;
            } else if (floodLevel < 50) {
                floodDirection = 1;
            }
            events.ingestObservation(new ReadingRequest("Sensor de nível S-021", "Sensores de nível",
                    EventType.ALAGAMENTO, "Porto Seco", round(floodLevel), floodLevel > 110 ? Boolean.FALSE : null, null,
                    null, null, null, null), SourceKind.SENSOR);

            heat = clamp(heat + (random.nextDouble() - 0.45) * 2.0, 28, 47);
            events.ingestObservation(new ReadingRequest("Estação meteorológica EM-05", "Estações meteorológicas",
                    EventType.CALOR, "Jardim das Flores", round(heat), null, heat > 40 ? Boolean.FALSE : Boolean.TRUE,
                    null, null, null, null), SourceKind.SENSOR);

            if (tick % 12 == 0) {   // a cada ~1 min mantém as fontes públicas "vivas"
                events.ingestObservation(new ReadingRequest("INMET – estação automática", "INMET – alertas meteorológicos",
                        EventType.CALOR, "Jardim das Flores", round(heat), null, Boolean.TRUE, null, null, null, null),
                        SourceKind.PUBLICA);
            }
        } catch (RuntimeException ex) {
            log.warn("Falha na simulação: {}", ex.getMessage());
        }
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
