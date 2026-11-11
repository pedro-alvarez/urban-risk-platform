package br.com.fiap.urbanrisk.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Dado recebido de uma fonte (sensor, base pública ou comunidade) que sustenta um evento. */
@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private RiskEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceKind kind;

    /** Nome da fonte específica (ex.: "Sensor de nível S-014"). Fontes distintas são evidências independentes. */
    @Column(nullable = false)
    private String sourceName;

    /** Grupo usado no monitoramento de saúde da fonte (ex.: "Sensores de nível"). */
    @Column(nullable = false)
    private String sourceGroup;

    @Column(nullable = false)
    private Instant receivedAt;

    /** Medição numérica opcional (nível em % do limite, índice de calor etc.). */
    private Double measurement;

    protected Evidence() {
    }

    public Evidence(RiskEvent event, SourceKind kind, String sourceName, String sourceGroup,
                    Instant receivedAt, Double measurement) {
        this.event = event;
        this.kind = kind;
        this.sourceName = sourceName;
        this.sourceGroup = sourceGroup;
        this.receivedAt = receivedAt;
        this.measurement = measurement;
    }

    public Long getId() { return id; }
    public RiskEvent getEvent() { return event; }
    public SourceKind getKind() { return kind; }
    public String getSourceName() { return sourceName; }
    public String getSourceGroup() { return sourceGroup; }
    public Instant getReceivedAt() { return receivedAt; }
    public Double getMeasurement() { return measurement; }
}
