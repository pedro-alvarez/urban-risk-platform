package br.com.fiap.urbanrisk.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.urbanrisk.domain.DataSourceEntry;

public interface DataSourceRepository extends JpaRepository<DataSourceEntry, Long> {

    Optional<DataSourceEntry> findByName(String name);
}
