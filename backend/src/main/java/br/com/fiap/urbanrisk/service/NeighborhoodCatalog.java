package br.com.fiap.urbanrisk.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Catálogo (fictício) dos bairros de Nova Aurora, com indicação de área vulnerável e coordenadas. */
@Component
public class NeighborhoodCatalog {

    public record Neighborhood(String name, boolean vulnerable, double lat, double lng) {
    }

    private final Map<String, Neighborhood> byName = new LinkedHashMap<>();

    public NeighborhoodCatalog() {
        add("Ribeira", true, -23.5480, -46.6420);
        add("Sol", true, -23.5390, -46.6180);
        add("Central", false, -23.5505, -46.6333);
        add("Vila Nova", true, -23.5610, -46.6450);
        add("Jardim das Flores", false, -23.5560, -46.6120);
        add("Porto Seco", false, -23.5400, -46.6300);
    }

    private void add(String name, boolean vulnerable, double lat, double lng) {
        byName.put(name, new Neighborhood(name, vulnerable, lat, lng));
    }

    public Neighborhood require(String name) {
        Neighborhood n = byName.get(name);
        if (n == null) {
            throw new IllegalArgumentException("Bairro desconhecido: " + name + ". Bairros válidos: " + byName.keySet());
        }
        return n;
    }

    public Iterable<Neighborhood> all() {
        return byName.values();
    }
}
