package br.com.fiap.urbanrisk.web.dto;

import java.time.Instant;
import java.util.List;

import br.com.fiap.urbanrisk.domain.ConfidenceLevel;
import br.com.fiap.urbanrisk.domain.EventStatus;
import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída da API REST. */
public final class Dtos {

    private Dtos() {
    }

    // ------------------------------------------------------------------ entrada

    /**
     * Leitura de sensor ou dado de base pública.
     * ALAGAMENTO: value = nível em % do limite de atenção (drainageOk opcional).
     * CALOR: value = índice de calor em °C (shelterAvailable opcional).
     * SERVICOS: powerOk, waterOk, communicationOk e helpRequested (value não é usado).
     */
    public record ReadingRequest(
            @NotBlank String sourceName,
            String sourceGroup,
            @NotNull EventType type,
            @NotBlank String neighborhood,
            Double value,
            Boolean drainageOk,
            Boolean shelterAvailable,
            Boolean powerOk,
            Boolean waterOk,
            Boolean communicationOk,
            Boolean helpRequested) {
    }

    /** Registro comunitário. Não coleta dados pessoais: apenas bairro e texto, com consentimento (LGPD). */
    public record CommunityReportRequest(
            @NotNull EventType type,
            @NotBlank String neighborhood,
            @NotNull RiskLevel severity,
            @NotBlank @Size(min = 10, max = 500) String description,
            boolean hasPhoto,
            boolean consent) {

        @AssertTrue(message = "É necessário consentir o tratamento dos dados para enviar o relato")
        public boolean isConsentGiven() {
            return consent;
        }
    }

    public record TransitionRequest(@NotNull String action, String actor) {
    }

    // ------------------------------------------------------------------ saída

    public record EvidenceView(String sourceName, String origin, String originLabel, Instant receivedAt,
                               Double measurement, int readings) {
    }

    public record HistoryView(EventStatus status, Instant at, String actor) {
    }

    public record TrendView(String direction, double slopePerMinute, Double minutesToLimit) {
    }

    public record EventView(
            Long id,
            String code,
            EventType type,
            String typeLabel,
            String neighborhood,
            boolean vulnerableArea,
            double lat,
            double lng,
            RiskLevel risk,
            EventStatus status,
            String title,
            String description,
            int confidence,
            ConfidenceLevel confidenceLevel,
            boolean staleSource,
            int priority,
            String origin,
            int evidenceCount,
            List<EvidenceView> evidences,
            List<HistoryView> history,
            TrendView trend,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record NotificationView(Long id, String eventCode, String neighborhood, RiskLevel level, String kind,
                                   String message, Instant createdAt) {
    }

    public record SourceView(String name, String origin, String originLabel, int periodSeconds, String state,
                             long secondsSinceUpdate, Instant lastUpdate) {
    }

    public record DashboardView(int activeEvents, int alertsIssued, int awaitingValidation, int averageConfidence,
                                int sourcesOnline, int sourcesTotal, Instant generatedAt) {
    }

    public record ErrorView(String error, String message, List<String> details) {
    }
}
