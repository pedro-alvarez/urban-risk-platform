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

/** Fonte de dados registrada, com período esperado de atualização (para medir o tempo de atualização). */
@Entity
@Table(name = "data_source_entry")
public class DataSourceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceKind kind;

    /** Período esperado entre atualizações, em segundos (0 = sob demanda, sempre online). */
    private int periodSeconds;

    @Column(nullable = false)
    private Instant lastUpdate;

    protected DataSourceEntry() {
    }

    public DataSourceEntry(String name, SourceKind kind, int periodSeconds, Instant lastUpdate) {
        this.name = name;
        this.kind = kind;
        this.periodSeconds = periodSeconds;
        this.lastUpdate = lastUpdate;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public SourceKind getKind() { return kind; }
    public int getPeriodSeconds() { return periodSeconds; }
    public Instant getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }
}
