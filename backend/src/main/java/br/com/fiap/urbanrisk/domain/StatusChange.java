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

/** Registro de auditoria de cada mudança de estado do evento. */
@Entity
@Table(name = "status_change")
public class StatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private RiskEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(nullable = false)
    private String actor;

    protected StatusChange() {
    }

    public StatusChange(RiskEvent event, EventStatus status, Instant changedAt, String actor) {
        this.event = event;
        this.status = status;
        this.changedAt = changedAt;
        this.actor = actor;
    }

    public Long getId() { return id; }
    public EventStatus getStatus() { return status; }
    public Instant getChangedAt() { return changedAt; }
    public String getActor() { return actor; }
}
