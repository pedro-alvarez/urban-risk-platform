package br.com.fiap.urbanrisk.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.urbanrisk.service.EventService;
import br.com.fiap.urbanrisk.service.NotificationService;
import br.com.fiap.urbanrisk.service.SourceHealthService;
import br.com.fiap.urbanrisk.web.dto.Dtos.DashboardView;
import br.com.fiap.urbanrisk.web.dto.Dtos.NotificationView;
import br.com.fiap.urbanrisk.web.dto.Dtos.SourceView;

/** Painel, notificações e saúde das fontes. */
@RestController
@RequestMapping("/api")
public class InfoController {

    private final EventService events;
    private final NotificationService notifications;
    private final SourceHealthService sources;

    public InfoController(EventService events, NotificationService notifications, SourceHealthService sources) {
        this.events = events;
        this.notifications = notifications;
        this.sources = sources;
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard() {
        return events.dashboard();
    }

    /** Sem parâmetro: últimas 50. Com afterId: apenas as novas (usado pelo app para alertar em tempo real). */
    @GetMapping("/notifications")
    public List<NotificationView> notifications(@RequestParam(required = false) Long afterId) {
        return notifications.list(afterId);
    }

    @GetMapping("/sources")
    public List<SourceView> sources() {
        return sources.list();
    }
}
