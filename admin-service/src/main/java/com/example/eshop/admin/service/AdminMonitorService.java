package com.example.eshop.admin.service;

import com.example.eshop.admin.dto.MonitorStatusResponse;
import com.example.eshop.admin.model.ServiceTick;
import com.example.eshop.admin.repository.ServiceTickRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records one monitor row per tick and deletes at most one expired row. */
@Service
public class AdminMonitorService {

    private static final Logger log = LoggerFactory.getLogger(AdminMonitorService.class);
    static final String STARTING = "STARTING";
    static final String SUCCESS_DETAIL = "tick";
    static final String FAILURE_DETAIL = "tick failed";

    private final ServiceTickRepository ticks;
    private final StaffSessionService staff;
    private final Clock clock;
    private final Duration retention;
    private final Duration staleAfter;

    public AdminMonitorService(ServiceTickRepository ticks, StaffSessionService staff, Clock clock,
                               @Value("${admin.monitor-retention:PT24H}") Duration retention,
                               @Value("${admin.monitor-delay-ms:30000}") long delayMs) {
        this.ticks = ticks;
        this.staff = staff;
        this.clock = clock;
        this.retention = retention;
        this.staleAfter = Duration.ofMillis(Math.multiplyExact(delayMs, 3));
    }

    @Scheduled(fixedDelayString = "${admin.monitor-delay-ms:30000}")
    @Transactional
    public void tick() {
        Instant started = Instant.now(clock);
        ServiceTick.Outcome outcome = ServiceTick.Outcome.SUCCESS;
        String detail = SUCCESS_DETAIL;
        try {
            ticks.findFirstByStartedAtBeforeOrderByStartedAtAsc(started.minus(retention))
                    .ifPresent(ticks::delete);
        } catch (Exception ex) {
            outcome = ServiceTick.Outcome.FAILED;
            detail = FAILURE_DETAIL;
            log.warn("Admin monitor tick failed");
        }
        ServiceTick row = new ServiceTick();
        row.setStartedAt(started);
        row.setFinishedAt(Instant.now(clock));
        row.setOutcome(outcome);
        row.setDetail(detail);
        ticks.save(row);
    }

    public boolean healthy() {
        return ticks.findFirstByOutcomeOrderByFinishedAtDesc(ServiceTick.Outcome.SUCCESS)
                .map(row -> !row.getFinishedAt().isBefore(Instant.now(clock).minus(staleAfter)))
                .orElse(true);
    }

    public MonitorStatusResponse current() {
        staff.requireStaff();
        return ticks.findFirstByOrderByStartedAtDesc()
                .map(row -> new MonitorStatusResponse(
                        row.getStartedAt(),
                        row.getFinishedAt(),
                        row.getOutcome().name(),
                        row.getDetail(),
                        healthy()))
                .orElseGet(() -> new MonitorStatusResponse(null, null, STARTING, "no tick yet", true));
    }
}
