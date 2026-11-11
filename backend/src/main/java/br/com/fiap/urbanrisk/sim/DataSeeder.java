package br.com.fiap.urbanrisk.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.repository.RiskEventRepository;
import br.com.fiap.urbanrisk.service.EventService;
import br.com.fiap.urbanrisk.service.SourceHealthService;
import br.com.fiap.urbanrisk.web.dto.Dtos.CommunityReportRequest;
import br.com.fiap.urbanrisk.web.dto.Dtos.ReadingRequest;

/** Carrega fontes e eventos de exemplo na primeira execução (banco vazio). */
@Component
@Order(1)
@ConditionalOnProperty(name = "urbanrisk.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EventService events;
    private final RiskEventRepository repository;
    private final SourceHealthService sources;

    public DataSeeder(EventService events, RiskEventRepository repository, SourceHealthService sources) {
        this.events = events;
        this.repository = repository;
        this.sources = sources;
    }

    @Override
    public void run(String... args) {
        sources.registerDefaults();
        if (repository.count() > 0) {
            return;
        }
        // Ribeira: nível do canal acima do limite (sensor + boletim da Defesa Civil) -> alerta
        events.ingestObservation(new ReadingRequest("Sensor de nível S-014", "Sensores de nível", EventType.ALAGAMENTO,
                "Ribeira", 108.0, null, null, null, null, null, null), SourceKind.SENSOR);
        events.ingestObservation(new ReadingRequest("Defesa Civil – boletim de chuva", "Boletins da Defesa Civil",
                EventType.ALAGAMENTO, "Ribeira", 105.0, null, null, null, null, null, null), SourceKind.PUBLICA);
        // Central: falha simultânea de energia e água -> crítico
        events.ingestObservation(new ReadingRequest("Concessionária – nota operacional", "Concessionária de energia",
                EventType.SERVICOS, "Central", null, null, null, false, false, true, false), SourceKind.PUBLICA);
        events.ingestObservation(new ReadingRequest("Medidor de pressão M-221", "Medidores de pressão de água",
                EventType.SERVICOS, "Central", null, null, null, false, false, true, false), SourceKind.SENSOR);
        // Sol: calor elevado -> alerta
        events.ingestObservation(new ReadingRequest("Estação meteorológica EM-03", "Estações meteorológicas",
                EventType.CALOR, "Sol", 41.0, null, true, null, null, null, null), SourceKind.SENSOR);
        // Porta Seco: poço em atenção, risco baixo -> monitoramento
        events.ingestObservation(new ReadingRequest("Sensor de nível S-031", "Sensores de nível", EventType.ALAGAMENTO,
                "Porto Seco", 65.0, null, null, null, null, null, null), SourceKind.SENSOR);
        // Relatos comunitários -> ficam em validação
        events.registerCommunityReport(new CommunityReportRequest(EventType.ALAGAMENTO, "Vila Nova", RiskLevel.MEDIO,
                "Água acumulada na rua principal, na altura do tornozelo.", false, true));
        events.registerCommunityReport(new CommunityReportRequest(EventType.SERVICOS, "Jardim das Flores",
                RiskLevel.MEDIO, "Pedido de socorro por rádio comunitário e sem sinal de celular na região.", false, true));
        log.info("Dados de exemplo carregados: {} eventos", repository.count());
    }
}
