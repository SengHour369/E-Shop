package com.example.eshop.auth.service;

import com.example.eshop.auth.model.FunctionPermission;
import com.example.eshop.auth.model.Group;
import com.example.eshop.auth.model.GroupPermission;
import com.example.eshop.auth.model.UserGroup;
import com.example.eshop.auth.model.UserPermission;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AiPermissionRepositoryTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_permissions;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Transactional
class AiPermissionRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = FunctionPermission.class)
    @EnableJpaRepositories(basePackageClasses = FunctionPermissionRepository.class,
            includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE, classes = FunctionPermissionRepository.class),
            excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                    type = FilterType.REGEX, pattern = "com.example.eshop.auth.repository.(?!FunctionPermissionRepository).*"))
    static class Config {
    }

    @Autowired
    EntityManager entities;

    @Autowired
    FunctionPermissionRepository functions;

    @Test
    void deletedDirectGrantsAndInactiveFunctionsAreExcluded() {
        var function = FunctionPermission.builder()
                .funcId(100L).funcCode("ORDER_VIEW_ALL").funcName("View orders").build();
        entities.persist(function);
        var grant = UserPermission.builder()
                .userId(7L).funcId(100L).isActive(true).isDelete(false).build();
        entities.persist(grant);
        assertThat(functions.effectiveCodes(7L)).containsExactly("ORDER_VIEW_ALL");
        grant.setIsDelete(true);
        assertThat(functions.effectiveCodes(7L)).isEmpty();
        grant.setIsDelete(false);
        function.setIsActive(false);
        assertThat(functions.effectiveCodes(7L)).isEmpty();
        assertThat(functions.effectiveCodes(8L)).isEmpty();
    }

    @Test
    void groupGrantRequiresActiveMembershipAndGroup() {
        var function = FunctionPermission.builder()
                .funcId(101L).funcCode("PAYMENT_VIEW_ALL").funcName("View payments").build();
        entities.persist(function);
        var group = new Group();
        group.setGroupCode("AI_TEST");
        group.setIsActive(true);
        group.setIsDelete(false);
        entities.persist(group);
        var membership = new UserGroup();
        membership.setUserId(7L);
        membership.setGroupId(group.getId());
        entities.persist(membership);
        entities.persist(GroupPermission.builder()
                .groupId(group.getId()).funcId(101L).isActive(true).isDelete(false).build());
        assertThat(functions.effectiveCodes(7L)).containsExactly("PAYMENT_VIEW_ALL");
        membership.setIsActive(false);
        assertThat(functions.effectiveCodes(7L)).isEmpty();
        membership.setIsActive(true);
        group.setIsDelete(true);
        assertThat(functions.effectiveCodes(7L)).isEmpty();
    }
}
