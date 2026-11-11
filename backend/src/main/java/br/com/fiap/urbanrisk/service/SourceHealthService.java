package br.com.fiap.urbanrisk.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.urbanrisk.domain.DataSourceEntry;
import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.repository.DataSourceRepository;
import br.com.fiap.urbanrisk.web.dto.Dtos.SourceView;

/** Monitora a saúde das fontes de dados (tempo de atualização e disponibilidade). */
@Service
public class SourceHealthService {

    public static final String ONLINE = "ONLINE";
    public static final String DEGRADED = "DEGRADADA";
    public static final String OFFLINE = "OFFLINE";

    private final DataSourceRepository repository;
    private final Clock clock;

    public SourceHealthService(DataSourceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void registerDefaults() {
        if (repository.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        repository.save(new DataSourceEntry("Sensores de nível", SourceKind.SENSOR, 30, now));
        repository.save(new DataSourceEntry("Estações meteorológicas", SourceKind.SENSOR, 60, now));
        repository.save(new DataSourceEntry("Medidores de pressão de água", SourceKind.SENSOR, 60, now));
        repository.save(new DataSourceEntry("Boletins da Defesa Civil", SourceKind.PUBLICA, 300, now));
        repository.save(new DataSourceEntry("INMET – alertas meteorológicos", SourceKind.PUBLICA, 600, now));
        repository.save(new DataSourceEntry("Concessionária de energia", SourceKind.PUBLICA, 300, now));
        repository.save(new DataSourceEntry("Registros comunitários", SourceKind.COMUNIDADE, 0, now));
    }

    /** Marca que a fonte acabou de enviar dado (cria a fonte se ainda não existir). */
    @Transactional
    public void touch(String group, SourceKind kind, Instant now) {
        DataSourceEntry entry = repository.findByName(group)
                .orElseGet(() -> new DataSourceEntry(group, kind, 60, now));
        entry.setLastUpdate(now);
        repository.save(entry);
    }

    /** RN-06: fonte offline = sem atualização há mais de 3 períodos esperados. */
    @Transactional(readOnly = true)
    public boolean isOffline(String group) {
        return repository.findByName(group)
                .map(s -> OFFLINE.equals(stateOf(s, clock.instant())))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<SourceView> list() {
        Instant now = clock.instant();
        return repository.findAll().stream().map(s -> new SourceView(
                s.getName(), s.getKind().name(), s.getKind().label(), s.getPeriodSeconds(), stateOf(s, now),
                Duration.between(s.getLastUpdate(), now).getSeconds(), s.getLastUpdate())).toList();
    }

    String stateOf(DataSourceEntry s, Instant now) {
        if (s.getPeriodSeconds() == 0) {
            return ONLINE;
        }
        long age = Duration.between(s.getLastUpdate(), now).getSeconds();
        if (age <= s.getPeriodSeconds() * 1.5) {
            return ONLINE;
        }
        return age <= s.getPeriodSeconds() * 3L ? DEGRADED : OFFLINE;
    }
}
