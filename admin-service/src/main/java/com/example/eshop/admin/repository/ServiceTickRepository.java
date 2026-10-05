package com.example.eshop.admin.repository;

import com.example.eshop.admin.model.ServiceTick;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTickRepository extends JpaRepository<ServiceTick, Long> {

    Optional<ServiceTick> findFirstByOrderByStartedAtDesc();

    Optional<ServiceTick> findFirstByOutcomeOrderByFinishedAtDesc(ServiceTick.Outcome outcome);

    Optional<ServiceTick> findFirstByStartedAtBeforeOrderByStartedAtAsc(Instant cutoff);
}
