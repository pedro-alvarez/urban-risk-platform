package br.com.fiap.urbanrisk.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Notificação gerada para os usuários (alerta, confirmação, encerramento). */
@Entity
@Table(name = "alert_notification")
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventCode;

    @Column(nullable = false)
    private String neighborhood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level;

    /** ALERTA, CONFIRMADO ou ENCERRADO. */
    @Column(nullable = false)
    private String kind;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected AlertNotification() {
    }

    public AlertNotification(String eventCode, String neighborhood, RiskLevel level, String kind,
                             String message, Instant createdAt) {
        this.eventCode = eventCode;
        this.neighborhood = neighborhood;
        this.level = level;
        this.kind = kind;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEventCode() { return eventCode; }
    public String getNeighborhood() { return neighborhood; }
    public RiskLevel getLevel() { return level; }
    public String getKind() { return kind; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
