package br.com.fiap.urbanrisk.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.urbanrisk.domain.SourceKind;
import br.com.fiap.urbanrisk.service.EventService;
import br.com.fiap.urbanrisk.web.dto.Dtos.CommunityReportRequest;
import br.com.fiap.urbanrisk.web.dto.Dtos.EventView;
import br.com.fiap.urbanrisk.web.dto.Dtos.ReadingRequest;
import jakarta.validation.Valid;

/** Entrada de dados: sensores, bases públicas e registros comunitários. */
@RestController
@RequestMapping("/api")
public class IngestController {

    private final EventService service;

    public IngestController(EventService service) {
        this.service = service;
    }

    @PostMapping("/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public EventView sensorReading(@Valid @RequestBody ReadingRequest request) {
        return service.ingestObservation(request, SourceKind.SENSOR);
    }

    @PostMapping("/public-data")
    @ResponseStatus(HttpStatus.CREATED)
    public EventView publicData(@Valid @RequestBody ReadingRequest request) {
        return service.ingestObservation(request, SourceKind.PUBLICA);
    }

    @PostMapping("/community-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public EventView communityReport(@Valid @RequestBody CommunityReportRequest request) {
        return service.registerCommunityReport(request);
    }
}
