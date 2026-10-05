package com.example.eshop.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eshop.admin.health.AdminMonitorHealthIndicator;
import com.example.eshop.admin.model.ServiceTick;
import com.example.eshop.admin.repository.ServiceTickRepository;
import com.example.eshop.admin.service.AdminMonitorService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "admin.monitor-enabled=false",
        "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"
})
@AutoConfigureMockMvc
class AdminMonitorTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AdminMonitorService monitor;

    @Autowired
    AdminMonitorHealthIndicator health;

    @Autowired
    ServiceTickRepository ticks;

    @BeforeEach
    void clean() {
        ticks.deleteAll();
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/admin/monitor")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/monitor")
                        .with(user("shopper").authorities(new SimpleGrantedAuthority("CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTickYetIsStartingAndHealthy() throws Exception {
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/monitor")
                        .with(user("ada").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("STARTING"))
                .andExpect(jsonPath("$.data.detail").value("no tick yet"))
                .andExpect(jsonPath("$.data.healthy").value(true));
    }

    @Test
    void tickWritesOneSuccessRow() throws Exception {
        monitor.tick();

        assertThat(ticks.count()).isEqualTo(1);
        ServiceTick row = ticks.findFirstByOrderByStartedAtDesc().orElseThrow();
        assertThat(row.getOutcome()).isEqualTo(ServiceTick.Outcome.SUCCESS);
        assertThat(row.getDetail()).isEqualTo("tick");
        assertThat(row.getFinishedAt()).isNotNull();
        mvc.perform(get("/api/v1/admin/monitor")
                        .with(user("mina").authorities(new SimpleGrantedAuthority("MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.data.detail").value("tick"))
                .andExpect(jsonPath("$.data.healthy").value(true));
    }

    @Test
    void tickDeletesOnlyTheOldestExpiredRow() {
        ServiceTick oldest = saveAt(Instant.now().minus(Duration.ofHours(30)));
        ServiceTick newer = saveAt(Instant.now().minus(Duration.ofHours(26)));

        monitor.tick();

        assertThat(ticks.existsById(oldest.getId())).isFalse();
        assertThat(ticks.existsById(newer.getId())).isTrue();
        assertThat(ticks.count()).isEqualTo(2);
    }

    @Test
    void staleSuccessIsDown() throws Exception {
        saveAt(Instant.now().minus(Duration.ofMinutes(5)));

        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
        mvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/admin/monitor")
                        .with(user("ada").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.data.healthy").value(false));
    }

    private ServiceTick saveAt(Instant started) {
        ServiceTick row = new ServiceTick();
        row.setStartedAt(started);
        row.setFinishedAt(started);
        row.setOutcome(ServiceTick.Outcome.SUCCESS);
        row.setDetail("tick");
        return ticks.save(row);
    }
}
