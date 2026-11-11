package br.com.fiap.urbanrisk.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.urbanrisk.domain.AlertNotification;
import br.com.fiap.urbanrisk.domain.RiskEvent;
import br.com.fiap.urbanrisk.repository.AlertNotificationRepository;
import br.com.fiap.urbanrisk.web.dto.Dtos.NotificationView;

/** Gera e consulta as notificações para moradores e agentes. */
@Service
public class NotificationService {

    public static final String KIND_ALERT = "ALERTA";
    public static final String KIND_CONFIRMED = "CONFIRMADO";
    public static final String KIND_CLOSED = "ENCERRADO";

    private final AlertNotificationRepository repository;
    private final Clock clock;

    public NotificationService(AlertNotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void alert(RiskEvent e) {
        save(e, KIND_ALERT, "ALERTA: risco " + e.getRiskLevel() + " – " + e.getType().label() + " em "
                + e.getNeighborhood() + ". " + e.getTitle() + ".");
    }

    @Transactional
    public void confirmed(RiskEvent e) {
        save(e, KIND_CONFIRMED, "Evento CONFIRMADO: " + e.getType().label() + " em " + e.getNeighborhood() + ".");
    }

    @Transactional
    public void closed(RiskEvent e) {
        save(e, KIND_CLOSED, "Encerrado: " + e.getType().label() + " em " + e.getNeighborhood()
                + ". Situação normalizada.");
    }

    private void save(RiskEvent e, String kind, String message) {
        repository.save(new AlertNotification(e.getCode(), e.getNeighborhood(), e.getRiskLevel(), kind, message,
                clock.instant()));
    }

    /** Se afterId for informado, devolve apenas as notificações mais novas (para polling do app). */
    @Transactional(readOnly = true)
    public List<NotificationView> list(Long afterId) {
        List<AlertNotification> list = afterId == null
                ? repository.findTop50ByOrderByIdDesc()
                : repository.findByIdGreaterThanOrderByIdAsc(afterId);
        return list.stream().map(n -> new NotificationView(n.getId(), n.getEventCode(), n.getNeighborhood(),
                n.getLevel(), n.getKind(), n.getMessage(), n.getCreatedAt())).toList();
    }
}
