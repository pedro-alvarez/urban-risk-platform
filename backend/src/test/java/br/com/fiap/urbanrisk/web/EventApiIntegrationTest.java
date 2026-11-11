package br.com.fiap.urbanrisk.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

/** Testes de integração da API (banco H2 em memória, sem simulador nem dados de exemplo). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "urbanrisk.simulator.enabled=false",
        "urbanrisk.seed.enabled=false"
})
@AutoConfigureMockMvc
class EventApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private MvcResult send(String url, String json, int expectedStatus) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    private static Number id(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void sensorReadingCreatesConfirmedEventAndEmitsAlertAutomatically() throws Exception {
        String body = """
                {"sourceName":"Sensor de nível T-01","sourceGroup":"Sensores de nível","type":"ALAGAMENTO",
                 "neighborhood":"Ribeira","value":112}
                """;
        mvc.perform(post("/api/readings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.risk", is("ALTO")))
                .andExpect(jsonPath("$.confidence", is(90)))
                .andExpect(jsonPath("$.confidenceLevel", is("ALTA")))
                .andExpect(jsonPath("$.origin", is("Sensor IoT")))
                .andExpect(jsonPath("$.status", is("ALERTA_EMITIDO")))
                .andExpect(jsonPath("$.code", notNullValue()));

        mvc.perform(get("/api/notifications")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind", is("ALERTA")));
    }

    @Test
    void independentSourcesRaiseConfidenceAndSameSourceDoesNot() throws Exception {
        String sensor = """
                {"sourceName":"Sensor T-02","sourceGroup":"Sensores de nível","type":"ALAGAMENTO","neighborhood":"Porto Seco","value":60}
                """;
        send("/api/readings", sensor, 201);
        send("/api/readings", sensor, 201);   // mesma fonte: não soma confiança
        String publicData = """
                {"sourceName":"Boletim T","sourceGroup":"Boletins da Defesa Civil","type":"ALAGAMENTO","neighborhood":"Porto Seco","value":62}
                """;
        mvc.perform(post("/api/public-data").contentType(MediaType.APPLICATION_JSON).content(publicData))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(100)))   // 90 + 15 (capped)
                .andExpect(jsonPath("$.evidences", hasSize(2)));
    }

    @Test
    void communityReportNeedsConsentAndStartsInValidationWithLowConfidence() throws Exception {
        String noConsent = """
                {"type":"ALAGAMENTO","neighborhood":"Vila Nova","severity":"MEDIO",
                 "description":"Água acumulada na rua principal","hasPhoto":false,"consent":false}
                """;
        send("/api/community-reports", noConsent, 400);

        String tooShort = """
                {"type":"ALAGAMENTO","neighborhood":"Vila Nova","severity":"MEDIO","description":"curto","consent":true}
                """;
        send("/api/community-reports", tooShort, 400);

        String ok = """
                {"type":"ALAGAMENTO","neighborhood":"Vila Nova","severity":"MEDIO",
                 "description":"Água acumulada na rua principal","hasPhoto":false,"consent":true}
                """;
        mvc.perform(post("/api/community-reports").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("EM_VALIDACAO")))
                .andExpect(jsonPath("$.confidence", is(40)))
                .andExpect(jsonPath("$.confidenceLevel", is("BAIXA")));
    }

    @Test
    void lowConfidenceEventCannotEmitAlertUntilMoreEvidenceArrives() throws Exception {
        String report = """
                {"type":"SERVICOS","neighborhood":"Jardim das Flores","severity":"ALTO",
                 "description":"Pedido de socorro e sem sinal de celular","consent":true}
                """;
        Number id = id(send("/api/community-reports", report, 201));
        // não pode alertar antes de confirmar
        send("/api/events/" + id + "/transitions", "{\"action\":\"ALERTAR\"}", 409);
        send("/api/events/" + id + "/transitions", "{\"action\":\"CONFIRMAR\",\"actor\":\"agente1\"}", 200);
        // confirmado, mas confiança 40% (Baixa): RN-04 bloqueia o alerta
        send("/api/events/" + id + "/transitions", "{\"action\":\"ALERTAR\"}", 422);
        // chega uma segunda fonte independente (base pública): confiança 55%
        String publicData = """
                {"sourceName":"Operadora","sourceGroup":"Concessionária de energia","type":"SERVICOS","neighborhood":"Jardim das Flores",
                 "communicationOk":false,"helpRequested":true}
                """;
        // evento já confirmado + confiança Média + risco Alto: o sistema emite o alerta automaticamente
        mvc.perform(post("/api/public-data").contentType(MediaType.APPLICATION_JSON).content(publicData))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(55)))
                .andExpect(jsonPath("$.status", is("ALERTA_EMITIDO")))
                .andExpect(jsonPath("$.history", hasSize(greaterThanOrEqualTo(4))));
        // o alerta já foi emitido: repetir a ação é uma transição inválida
        send("/api/events/" + id + "/transitions", "{\"action\":\"ALERTAR\"}", 409);
    }

    @Test
    void invalidInputsAreRejectedWithClearErrors() throws Exception {
        send("/api/readings", "{\"sourceName\":\"x\",\"type\":\"ALAGAMENTO\",\"neighborhood\":\"Marte\",\"value\":10}", 400);
        send("/api/readings", "{\"sourceName\":\"x\",\"type\":\"ALAGAMENTO\",\"neighborhood\":\"Ribeira\"}", 400);   // sem value
        send("/api/readings", "{\"type\":\"CALOR\"}", 400);
        mvc.perform(get("/api/events/999999")).andExpect(status().isNotFound());
        Number id = id(send("/api/readings",
                "{\"sourceName\":\"S\",\"type\":\"CALOR\",\"neighborhood\":\"Sol\",\"value\":36}", 201));
        send("/api/events/" + id + "/transitions", "{\"action\":\"VOAR\"}", 400);
    }

    @Test
    void eventsAreSortedByPriorityAndDashboardSummarizes() throws Exception {
        send("/api/readings", "{\"sourceName\":\"Sa\",\"type\":\"CALOR\",\"neighborhood\":\"Central\",\"value\":33}", 201);
        send("/api/readings", "{\"sourceName\":\"Sb\",\"type\":\"CALOR\",\"neighborhood\":\"Sol\",\"value\":47,\"shelterAvailable\":false}", 201);
        mvc.perform(get("/api/events")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priority", greaterThanOrEqualTo(80)));
        MvcResult r = mvc.perform(get("/api/events")).andReturn();
        java.util.List<Integer> priorities = JsonPath.read(r.getResponse().getContentAsString(), "$[*].priority");
        for (int i = 1; i < priorities.size(); i++) {
            org.junit.jupiter.api.Assertions.assertTrue(priorities.get(i - 1) >= priorities.get(i));
        }
        mvc.perform(get("/api/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEvents", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.sourcesTotal", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/sources")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
    }
}
