package br.com.fiap.urbanrisk.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.fiap.urbanrisk.domain.EventStatus;
import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.RiskEvent;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    /** Eventos abertos do mesmo tipo e bairro atualizados recentemente (RN-07: evita duplicidade). */
    @Query("select e from RiskEvent e where e.type = :type and e.neighborhood = :neighborhood "
            + "and e.status not in :closed and e.updatedAt >= :since order by e.updatedAt desc")
    List<RiskEvent> findOpen(@Param("type") EventType type,
                             @Param("neighborhood") String neighborhood,
                             @Param("closed") Collection<EventStatus> closed,
                             @Param("since") Instant since);
}
