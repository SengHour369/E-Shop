package com.example.eshop.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eshop.admin.model.ServiceTick;
import com.example.eshop.admin.repository.ServiceTickRepository;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminMonitorFailureTest {

    @Test
    void failedTickStoresAShortDetail() {
        ServiceTickRepository ticks = mock(ServiceTickRepository.class);
        when(ticks.findFirstByStartedAtBeforeOrderByStartedAtAsc(any()))
                .thenThrow(new IllegalStateException("password=hidden-value"));
        AdminMonitorService service = new AdminMonitorService(
                ticks, mock(StaffSessionService.class), Clock.systemUTC(), Duration.ofHours(24), 30_000);

        service.tick();

        ArgumentCaptor<ServiceTick> saved = ArgumentCaptor.forClass(ServiceTick.class);
        verify(ticks).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo(ServiceTick.Outcome.FAILED);
        assertThat(saved.getValue().getDetail()).isEqualTo("tick failed");
        assertThat(saved.getValue().getDetail()).doesNotContain("password");
    }
}
