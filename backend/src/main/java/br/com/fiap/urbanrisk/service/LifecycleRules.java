package br.com.fiap.urbanrisk.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.EventStatus;

/** Máquina de estados do evento de risco (diagrama de estados da documentação). */
@Component
public class LifecycleRules {

    public enum Action { VALIDAR, CONFIRMAR, ALERTAR, MONITORAR, ENCERRAR, DESCARTAR }

    private static final Map<EventStatus, Map<Action, EventStatus>> TRANSITIONS = new EnumMap<>(EventStatus.class);

    static {
        TRANSITIONS.put(EventStatus.REPORTADO, Map.of(
                Action.VALIDAR, EventStatus.EM_VALIDACAO,
                Action.DESCARTAR, EventStatus.DESCARTADO));
        TRANSITIONS.put(EventStatus.EM_VALIDACAO, Map.of(
                Action.CONFIRMAR, EventStatus.CONFIRMADO,
                Action.DESCARTAR, EventStatus.DESCARTADO));
        TRANSITIONS.put(EventStatus.CONFIRMADO, Map.of(
                Action.ALERTAR, EventStatus.ALERTA_EMITIDO,
                Action.MONITORAR, EventStatus.EM_MONITORAMENTO));
        TRANSITIONS.put(EventStatus.ALERTA_EMITIDO, Map.of(
                Action.MONITORAR, EventStatus.EM_MONITORAMENTO));
        TRANSITIONS.put(EventStatus.EM_MONITORAMENTO, Map.of(
                Action.ALERTAR, EventStatus.ALERTA_EMITIDO,
                Action.ENCERRAR, EventStatus.ENCERRADO));
        TRANSITIONS.put(EventStatus.ENCERRADO, Map.of());
        TRANSITIONS.put(EventStatus.DESCARTADO, Map.of());
    }

    public Optional<EventStatus> next(EventStatus from, Action action) {
        return Optional.ofNullable(TRANSITIONS.get(from).get(action));
    }

    public Map<Action, EventStatus> allowedFrom(EventStatus from) {
        return TRANSITIONS.get(from);
    }
}
