package com.example.eshop.common.audit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.slf4j.MDC;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import static org.assertj.core.api.Assertions.*;
@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AuditIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditIntegrationTest {
    @Configuration @EntityScan(basePackageClasses = AuditLog.class)
    @EnableJpaRepositories(basePackageClasses = AuditLogRepository.class)
    @Import({AuditLogService.class, AuditContextProvider.class, AuditSnapshots.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired AuditLogService service;
    @Autowired AuditLogRepository repository;
    @Autowired PlatformTransactionManager manager;
    @Autowired AuditSnapshots snapshots;
    @AfterEach void clean() { MDC.clear(); SecurityContextHolder.clearContext(); RequestContextHolder.resetRequestAttributes(); }
    private AuditSearch search(String id) {
        return new AuditSearch(id, null, null, null, null, null, null, null, null, null, null);
    }
    @Test void capturesActorRequestResourceAndSafeBeforeAfter() {
        MDC.put("requestId", "req_capture"); MDC.put("traceId", "trace-1");
        var auth = new UsernamePasswordAuthenticationToken("name", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(Map.of("userId", 25L)); SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest(); request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "spoofed"); request.addHeader("User-Agent", "test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        new TransactionTemplate(manager).executeWithoutResult(t -> service.record(AuditEvent.success(
            AuditAction.PROMOTION_UPDATE, "PROMOTION", 10, Map.of("discountValue", 10), Map.of("discountValue", 20, "password", "never-store"))));
        var rows = repository.findAll(search("req_capture").specification());
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.getActorId()).isEqualTo("25"); assertThat(row.getActorType()).isEqualTo(AuditActorType.ADMIN);
        assertThat(row.getTraceId()).isEqualTo("trace-1"); assertThat(row.getResourceId()).isEqualTo("10");
        assertThat(row.getIpAddress()).isEqualTo("10.0.0.2");
        assertThat(row.getOldValue()).contains("10");
        assertThat(row.getNewValue()).contains("20", "[REDACTED]").doesNotContain("never-store");
    }
    @Test void rollbackRemovesSuccessButIndependentFailureSurvives() {
        MDC.put("requestId", "req_rollback");
        new TransactionTemplate(manager).executeWithoutResult(t -> {
            service.record(AuditEvent.success(AuditAction.PRODUCT_UPDATE, "PRODUCT", 9, null, null));
            service.recordSecurity(AuditAction.LOGIN_FAILED, AuditResult.FAILURE, null, "AUTHENTICATION_FAILED");
            t.setRollbackOnly();
        });
        var rows = repository.findAll(search("req_rollback").specification());
        assertThat(rows).hasSize(1); assertThat(rows.get(0).getResult()).isEqualTo(AuditResult.FAILURE);
    }
    @Test void combinedFiltersAndPagination() {
        MDC.put("requestId", "req_search");
        new TransactionTemplate(manager).executeWithoutResult(t -> {
            for (int i=0; i<3; i++) service.record(AuditEvent.success(AuditAction.SKU_UPDATE, "PRODUCT_SKU", i, null, null));
        });
        var filter = new AuditSearch("req_search", null, null, AuditActorType.SYSTEM, AuditAction.SKU_UPDATE,
                "PRODUCT_SKU", null, AuditResult.SUCCESS, "unknown", Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));
        var page = repository.findAll(filter.specification(), PageRequest.of(1, 2, Sort.by("id")));
        assertThat(page.getTotalElements()).isEqualTo(3); assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getResourceId()).isEqualTo("2");
    }
    @Test void rejectsObjectGraphsAndRedactsSecrets() {
        String json = snapshots.json(Map.of("passwordHash", "hash-secret", "accessToken", "bearer-secret",
            "OTP", "123456", "privateKey", "private-secret", "status", Map.of("secret", "nested-secret")));
        assertThat(json).doesNotContain("hash-secret", "bearer-secret", "123456", "private-secret", "nested-secret");
    }
    @Test void successRequiresBusinessTransaction() {
        assertThatThrownBy(() -> service.record(AuditEvent.success(AuditAction.UPDATE, "PRODUCT", 1, null, null)))
            .isInstanceOf(IllegalTransactionStateException.class);
    }
}
