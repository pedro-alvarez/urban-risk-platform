package br.com.fiap.urbanrisk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.urbanrisk.domain.AlertNotification;

public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    List<AlertNotification> findTop50ByOrderByIdDesc();

    List<AlertNotification> findByIdGreaterThanOrderByIdAsc(Long id);
}
