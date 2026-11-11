package br.com.fiap.urbanrisk.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.urbanrisk.domain.ConfidenceLevel;
import br.com.fiap.urbanrisk.domain.EventStatus;
import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.Evidence;
import br.com.fiap.urbanrisk.domain.RiskEvent;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.domain.StatusChange;
import br.com.fiap.urbanrisk.repository.RiskEventRepository;
import br.com.fiap.urbanrisk.service.LifecycleRules.Action;
import br.com.fiap.urbanrisk.service.NeighborhoodCatalog.Neighborhood;
import br.com.fiap.urbanrisk.web.dto.Dtos.CommunityReportRequest;
import br.com.fiap.urbanrisk.web.dto.Dtos.DashboardView;
import br.com.fiap.urbanrisk.web.dto.Dtos.EventView;
import br.com.fiap.urbanrisk.web.dto.Dtos.EvidenceView;
import br.com.fiap.urbanrisk.web.dto.Dtos.HistoryView;
import br.com.fiap.urbanrisk.web.dto.Dtos.ReadingRequest;
import br.com.fiap.urbanrisk.web.dto.Dtos.SourceView;
import br.com.fiap.urbanrisk.web.dto.Dtos.TrendView;
import br.com.fiap.urbanrisk.web.error.BusinessRuleException;

/** Núcleo da plataforma: ingestão, validação, priorização e ciclo de vida dos eventos de risco. */
@Service
public class EventService {

    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(30);           // RN-07
    private static final List<EventStatus> CLOSED = List.of(EventStatus.ENCERRADO, EventStatus.DESCARTADO);
    private static final int MAX_MEASUREMENTS_PER_SOURCE = 50;
    private static final String SYSTEM = "sistema";

    private final RiskEventRepository events;
    private final NeighborhoodCatalog catalog;
    private final ConfidenceCalculator confidenceCalculator;
    private final PriorityCalculator priorityCalculator;
    private final RiskClassifier classifier;
    private final TrendAnalyzer trendAnalyzer;
    private final LifecycleRules rules;
    private final NotificationService notifications;
    private final SourceHealthService sources;
    private final Clock clock;

    public EventService(RiskEventRepository events, NeighborhoodCatalog catalog,
                        ConfidenceCalculator confidenceCalculator, PriorityCalculator priorityCalculator,
                        RiskClassifier classifier, TrendAnalyzer trendAnalyzer, LifecycleRules rules,
                        NotificationService notifications, SourceHealthService sources, Clock clock) {
        this.events = events;
        this.catalog = catalog;
        this.confidenceCalculator = confidenceCalculator;
        this.priorityCalculator = priorityCalculator;
        this.classifier = classifier;
        this.trendAnalyzer = trendAnalyzer;
        this.rules = rules;
        this.notifications = notifications;
        this.sources = sources;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ ingestão

    /** Leitura de sensor (SENSOR) ou dado de base pública (PUBLICA). */
    @Transactional
    public EventView ingestObservation(ReadingRequest r, SourceKind kind) {
        Neighborhood n = catalog.require(r.neighborhood());
        RiskLevel risk = classifier.classify(r, n.vulnerable());
        String group = r.sourceGroup() != null && !r.sourceGroup().isBlank() ? r.sourceGroup() : r.sourceName();
        return ingest(r.type(), n, risk, kind, r.sourceName(), group, r.value(), titleFor(r, risk), descriptionFor(r));
    }

    /** Registro comunitário: sem dados pessoais; exige consentimento (validado no DTO). */
    @Transactional
    public EventView registerCommunityReport(CommunityReportRequest r) {
        if (!r.consent()) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, "Consentimento obrigatório (LGPD)");
        }
        Neighborhood n = catalog.require(r.neighborhood());
        RiskLevel risk = r.severity() == RiskLevel.CRITICO ? RiskLevel.ALTO : r.severity();
        String title = "Relato comunitário: " + r.type().label().toLowerCase(Locale.ROOT);
        return ingest(r.type(), n, risk, SourceKind.COMUNIDADE, "Registro comunitário", "Registros comunitários",
                null, title, r.description().trim());
    }

    private EventView ingest(EventType type, Neighborhood n, RiskLevel risk, SourceKind kind, String sourceName,
                             String sourceGroup, Double measurement, String title, String description) {
        Instant now = clock.instant();
        List<RiskEvent> open = events.findOpen(type, n.name(), CLOSED, now.minus(DEDUP_WINDOW));
        boolean created = open.isEmpty();
        RiskEvent e;
        if (created) {
            e = new RiskEvent(type, n.name(), n.vulnerable(), risk, title, description, now);
            e = events.save(e);
            e.setCode("EV-" + (1000 + e.getId()));
            changeStatus(e, EventStatus.REPORTADO, SYSTEM, now);
        } else {
            e = open.get(0);
        }
        e.addEvidence(new Evidence(e, kind, sourceName, sourceGroup, now, measurement));
        if (!created) {
            if (kind == SourceKind.SENSOR) {      // a leitura mais recente do sensor define o risco atual
                e.setRiskLevel(risk);
                e.setTitle(title);
                e.setDescription(description);
            } else {
                e.setRiskLevel(RiskLevel.max(e.getRiskLevel(), risk));
            }
        }
        e.setUpdatedAt(now);
        pruneMeasurements(e, sourceName);
        sources.touch(sourceGroup, kind, now);
        autoProgress(e, now);
        return toView(e);
    }

    /**
     * Avanço automático: dado com confiança alta (>= 80) é confirmado sem intervenção humana; confirmado com
     * risco >= Médio dispara alerta (RN-04); risco Baixo vai para monitoramento. Dado com confiança menor fica
     * "Em validação" até o agente validar ou chegar nova evidência independente.
     */
    private void autoProgress(RiskEvent e, Instant now) {
        if (e.getStatus() == EventStatus.REPORTADO) {
            changeStatus(e, EventStatus.EM_VALIDACAO, SYSTEM, now);
        }
        int confidence = confidenceOf(e);
        if (e.getStatus() == EventStatus.EM_VALIDACAO && confidence >= 80) {
            changeStatus(e, EventStatus.CONFIRMADO, SYSTEM, now);
            notifications.confirmed(e);
        }
        if (e.getStatus() == EventStatus.CONFIRMADO) {
            if (e.getRiskLevel().atLeast(RiskLevel.MEDIO)) {
                changeStatus(e, EventStatus.ALERTA_EMITIDO, SYSTEM, now);
                notifications.alert(e);
            } else {
                changeStatus(e, EventStatus.EM_MONITORAMENTO, SYSTEM, now);
            }
        } else if (e.getStatus() == EventStatus.EM_MONITORAMENTO && e.getRiskLevel().atLeast(RiskLevel.ALTO)
                && confidence >= 80) {
            changeStatus(e, EventStatus.ALERTA_EMITIDO, SYSTEM, now);      // risco escalou
            notifications.alert(e);
        }
    }

    private void pruneMeasurements(RiskEvent e, String sourceName) {
        List<Evidence> same = e.getEvidences().stream()
                .filter(v -> v.getMeasurement() != null && v.getSourceName().equals(sourceName)).toList();
        int excess = same.size() - MAX_MEASUREMENTS_PER_SOURCE;
        if (excess > 0) {
            // remove as medições mais antigas, mantendo sempre a primeira evidência do evento (fonte principal)
            same.stream().skip(1).limit(excess).forEach(v -> e.getEvidences().remove(v));
        }
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Transactional
    public EventView transition(Long id, String actionName, String actor) {
        RiskEvent e = find(id);
        Action action;
        try {
            action = Action.valueOf(actionName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Ação desconhecida: " + actionName + ". Ações: "
                    + java.util.Arrays.toString(Action.values()));
        }
        EventStatus from = e.getStatus();
        EventStatus next = rules.next(from, action).orElseThrow(() -> new BusinessRuleException(HttpStatus.CONFLICT,
                "Transição inválida: não é possível " + action + " um evento " + from));
        if (action == Action.ALERTAR) {
            checkCanAlert(e, from);
        }
        Instant now = clock.instant();
        changeStatus(e, next, actor == null || actor.isBlank() ? "agente" : actor, now);
        e.setUpdatedAt(now);
        switch (next) {
            case CONFIRMADO -> notifications.confirmed(e);
            case ALERTA_EMITIDO -> notifications.alert(e);
            case ENCERRADO -> notifications.closed(e);
            default -> { }
        }
        return toView(e);
    }

    /** RN-04: alerta só para evento confirmado, risco suficiente e confiança não baixa. */
    private void checkCanAlert(RiskEvent e, EventStatus from) {
        RiskLevel minimum = from == EventStatus.EM_MONITORAMENTO ? RiskLevel.ALTO : RiskLevel.MEDIO;
        if (!e.getRiskLevel().atLeast(minimum)) {
            throw new BusinessRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Risco " + e.getRiskLevel() + " insuficiente para emitir alerta (mínimo: " + minimum + ")");
        }
        if (ConfidenceLevel.of(confidenceOf(e)) == ConfidenceLevel.BAIXA) {
            throw new BusinessRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Confiança baixa: obtenha uma evidência adicional antes de emitir alerta (RN-04)");
        }
    }

    private void changeStatus(RiskEvent e, EventStatus status, String actor, Instant now) {
        e.setStatus(status);
        e.addHistory(new StatusChange(e, status, now, actor));
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<EventView> list(EventType type, RiskLevel risk, EventStatus status, boolean onlyOpen) {
        return events.findAll().stream()
                .filter(e -> type == null || e.getType() == type)
                .filter(e -> risk == null || e.getRiskLevel() == risk)
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> !onlyOpen || e.getStatus().isOpen())
                .map(this::toView)
                .sorted(Comparator.comparingInt(EventView::priority).reversed()
                        .thenComparing(EventView::updatedAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventView get(Long id) {
        return toView(find(id));
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        List<EventView> active = list(null, null, null, true);
        int alerts = (int) active.stream().filter(v -> v.status() == EventStatus.ALERTA_EMITIDO).count();
        int waiting = (int) active.stream()
                .filter(v -> v.status() == EventStatus.REPORTADO || v.status() == EventStatus.EM_VALIDACAO).count();
        int avg = active.isEmpty() ? 0 : (int) Math.round(active.stream().mapToInt(EventView::confidence).average().orElse(0));
        List<SourceView> src = sources.list();
        int online = (int) src.stream().filter(s -> SourceHealthService.ONLINE.equals(s.state())).count();
        return new DashboardView(active.size(), alerts, waiting, avg, online, src.size(), clock.instant());
    }

    private RiskEvent find(Long id) {
        return events.findById(id).orElseThrow(() -> new NoSuchElementException("Evento não encontrado: " + id));
    }

    private int confidenceOf(RiskEvent e) {
        List<Evidence> evs = e.getEvidences();
        boolean stale = !evs.isEmpty() && sources.isOffline(evs.get(0).getSourceGroup());
        return confidenceCalculator.calculate(evs, stale);
    }

    // ------------------------------------------------------------------ mapeamento

    private EventView toView(RiskEvent e) {
        List<Evidence> evs = e.getEvidences();
        boolean stale = !evs.isEmpty() && sources.isOffline(evs.get(0).getSourceGroup());
        int confidence = confidenceCalculator.calculate(evs, stale);
        TrendView trend = trendAnalyzer.analyze(evs, e.getType());
        int priority = priorityCalculator.calculate(e.getRiskLevel(), confidence, e.isVulnerableArea(),
                TrendAnalyzer.RISING.equals(trend.direction()));

        Map<String, List<Evidence>> bySource = evs.stream()
                .collect(Collectors.groupingBy(Evidence::getSourceName, LinkedHashMap::new, Collectors.toList()));
        List<EvidenceView> evidenceViews = bySource.values().stream().map(list -> {
            Evidence last = list.get(list.size() - 1);
            return new EvidenceView(last.getSourceName(), last.getKind().name(), last.getKind().label(),
                    last.getReceivedAt(), last.getMeasurement(), list.size());
        }).toList();
        List<HistoryView> history = e.getHistory().stream()
                .map(h -> new HistoryView(h.getStatus(), h.getChangedAt(), h.getActor())).toList();
        Neighborhood n = catalog.require(e.getNeighborhood());
        String origin = evs.isEmpty() ? "-" : evs.get(0).getKind().label();
        return new EventView(e.getId(), e.getCode(), e.getType(), e.getType().label(), e.getNeighborhood(),
                e.isVulnerableArea(), n.lat(), n.lng(), e.getRiskLevel(), e.getStatus(), e.getTitle(),
                e.getDescription(), confidence, ConfidenceLevel.of(confidence), stale, priority, origin,
                evs.size(), evidenceViews, history, trend, e.getCreatedAt(), e.getUpdatedAt());
    }

    // ------------------------------------------------------------------ textos

    private static String titleFor(ReadingRequest r, RiskLevel risk) {
        return switch (r.type()) {
            case ALAGAMENTO -> "Nível do canal em " + Math.round(r.value()) + "% do limite de atenção";
            case CALOR -> "Índice de calor de " + Math.round(r.value()) + " °C";
            case SERVICOS -> "Interrupção de serviços essenciais";
        };
    }

    private static String descriptionFor(ReadingRequest r) {
        StringBuilder sb = new StringBuilder();
        switch (r.type()) {
            case ALAGAMENTO -> {
                sb.append("Nível medido em ").append(Math.round(r.value())).append("% do limite de atenção.");
                if (Boolean.FALSE.equals(r.drainageOk())) {
                    sb.append(" Drenagem inoperante.");
                }
            }
            case CALOR -> {
                sb.append("Índice de calor de ").append(Math.round(r.value())).append(" °C.");
                if (Boolean.FALSE.equals(r.shelterAvailable())) {
                    sb.append(" Nenhum abrigo climático disponível.");
                }
            }
            case SERVICOS -> {
                if (Boolean.FALSE.equals(r.powerOk())) {
                    sb.append("Energia elétrica indisponível. ");
                }
                if (Boolean.FALSE.equals(r.waterOk())) {
                    sb.append("Abastecimento de água indisponível. ");
                }
                if (Boolean.FALSE.equals(r.communicationOk())) {
                    sb.append("Comunicação local indisponível. ");
                }
                if (Boolean.TRUE.equals(r.helpRequested())) {
                    sb.append("Há pedido de socorro registrado.");
                }
            }
        }
        return sb.toString().trim();
    }
}
