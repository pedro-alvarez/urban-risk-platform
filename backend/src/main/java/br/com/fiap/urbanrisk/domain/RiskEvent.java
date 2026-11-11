package br.com.fiap.urbanrisk.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Evento de risco urbano: agrega evidências de várias fontes. */
@Entity
@Table(name = "risk_event")
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private String neighborhood;

    private boolean vulnerableArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("receivedAt ASC, id ASC")
    private List<Evidence> evidences = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<StatusChange> history = new ArrayList<>();

    protected RiskEvent() {
    }

    public RiskEvent(EventType type, String neighborhood, boolean vulnerableArea, RiskLevel riskLevel,
                     String title, String description, Instant now) {
        this.type = type;
        this.neighborhood = neighborhood;
        this.vulnerableArea = vulnerableArea;
        this.riskLevel = riskLevel;
        this.title = title;
        this.description = description;
        this.status = EventStatus.REPORTADO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addEvidence(Evidence evidence) {
        evidences.add(evidence);
    }

    public void addHistory(StatusChange change) {
        history.add(change);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public EventType getType() { return type; }
    public String getNeighborhood() { return neighborhood; }
    public boolean isVulnerableArea() { return vulnerableArea; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<Evidence> getEvidences() { return evidences; }
    public List<StatusChange> getHistory() { return history; }
}
