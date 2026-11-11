package br.com.fiap.urbanrisk.service;

import org.springframework.stereotype.Component;

import br.com.fiap.urbanrisk.domain.EventType;
import br.com.fiap.urbanrisk.domain.RiskLevel;
import br.com.fiap.urbanrisk.web.dto.Dtos.ReadingRequest;

/** RN-05: classificação do nível de risco por tipo de evento. */
@Component
public class RiskClassifier {

    public RiskLevel classify(ReadingRequest r, boolean vulnerableArea) {
        return switch (r.type()) {
            case ALAGAMENTO -> flood(require(r.value(), "value (nível em % do limite)"), r.drainageOk(), vulnerableArea);
            case CALOR -> heat(require(r.value(), "value (índice de calor em °C)"), r.shelterAvailable(), vulnerableArea);
            case SERVICOS -> services(r.powerOk(), r.waterOk(), r.communicationOk(), r.helpRequested());
        };
    }

    /** Alagamento: nível em % do limite de atenção. */
    public RiskLevel flood(double levelPercent, Boolean drainageOk, boolean vulnerableArea) {
        RiskLevel level;
        if (levelPercent < 80) {
            level = RiskLevel.BAIXO;
        } else if (levelPercent <= 100) {
            level = RiskLevel.MEDIO;
        } else if (levelPercent <= 120) {
            level = RiskLevel.ALTO;
        } else {
            level = RiskLevel.CRITICO;
        }
        // drenagem inoperante em área vulnerável agrava o risco quando o nível já exige atenção
        if (Boolean.FALSE.equals(drainageOk) && vulnerableArea && levelPercent >= 80) {
            level = level.up();
        }
        return level;
    }

    /** Calor: índice de calor em °C. */
    public RiskLevel heat(double heatIndex, Boolean shelterAvailable, boolean vulnerableArea) {
        RiskLevel level;
        if (heatIndex < 32) {
            level = RiskLevel.BAIXO;
        } else if (heatIndex <= 38) {
            level = RiskLevel.MEDIO;
        } else if (heatIndex <= 45) {
            level = RiskLevel.ALTO;
        } else {
            level = RiskLevel.CRITICO;
        }
        // sem abrigo em área vulnerável sobe um nível (mesma lógica do módulo Escudo do Calor)
        if (Boolean.FALSE.equals(shelterAvailable) && vulnerableArea && heatIndex >= 32) {
            level = level.up();
        }
        return level;
    }

    /** Serviços essenciais (mesma lógica do módulo Pulso dos Serviços do projeto de Digital Systems Lab). */
    public RiskLevel services(Boolean powerOk, Boolean waterOk, Boolean communicationOk, Boolean helpRequested) {
        boolean noPower = Boolean.FALSE.equals(powerOk);
        boolean noWater = Boolean.FALSE.equals(waterOk);
        boolean noComm = Boolean.FALSE.equals(communicationOk);
        boolean help = Boolean.TRUE.equals(helpRequested);
        if (noPower && noWater) {
            return RiskLevel.CRITICO;
        }
        if (help && noComm) {
            return RiskLevel.ALTO;
        }
        if (noPower || noWater || noComm) {
            return RiskLevel.MEDIO;
        }
        return RiskLevel.BAIXO;
    }

    private static double require(Double value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("Campo obrigatório para este tipo de evento: " + name);
        }
        return value;
    }
}
