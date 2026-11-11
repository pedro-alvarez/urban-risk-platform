package br.com.fiap.urbanrisk.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.urbanrisk.domain.EventStatus;
import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import br.com.fiap.urbanrisk.service.EventService;
import br.com.fiap.urbanrisk.web.dto.Dtos.EventView;
import br.com.fiap.urbanrisk.web.dto.Dtos.TransitionRequest;
import jakarta.validation.Valid;

/** Consulta de eventos priorizados e ações do ciclo de vida. */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    /** Eventos ordenados por prioridade (maior primeiro). Por padrão, apenas os abertos. */
    @GetMapping
    public List<EventView> list(@RequestParam(required = false) EventType type,
                                @RequestParam(required = false) RiskLevel risk,
                                @RequestParam(required = false) EventStatus status,
                                @RequestParam(defaultValue = "true") boolean onlyOpen) {
        return service.list(type, risk, status, onlyOpen && status == null);
    }

    @GetMapping("/{id}")
    public EventView get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Ações: VALIDAR, CONFIRMAR, ALERTAR, MONITORAR, ENCERRAR, DESCARTAR. */
    @PostMapping("/{id}/transitions")
    public EventView transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return service.transition(id, request.action(), request.actor());
    }
}
