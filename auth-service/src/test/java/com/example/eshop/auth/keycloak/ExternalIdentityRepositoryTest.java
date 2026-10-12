package com.example.eshop.auth.keycloak;

import com.example.eshop.auth.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExternalIdentityRepositoryTest.Config.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:keycloak_identity;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"
})
@Transactional
class ExternalIdentityRepositoryTest {
    @Configuration @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ExternalIdentity.class, User.class})
    @EnableJpaRepositories(basePackageClasses = ExternalIdentityRepository.class)
    static class Config {}
    @Autowired EntityManager entities;
    @Autowired ExternalIdentityRepository identities;
    @Test void resolvesExactIssuerAndSubjectToExistingLocalUser() {
        User user = User.builder().username("existing").email("existing@eshop.local").password("unused")
                .phone("test-phone").status("ACT").enabled(true).deleted(false).roles(List.of()).build();
        entities.persist(user);
        entities.flush();
        entities.createNativeQuery("INSERT INTO external_identity(issuer,subject,user_id) VALUES (?1,?2,?3)")
                .setParameter(1, "https://issuer.test/realms/eshop").setParameter(2, "verified-subject")
                .setParameter(3, user.getId()).executeUpdate();
        assertThat(identities.findByIssuerAndSubject("https://issuer.test/realms/eshop", "verified-subject")
                .orElseThrow().getUser().getId()).isEqualTo(user.getId());
        assertThat(identities.findByIssuerAndSubject("https://other.test", "verified-subject")).isEmpty();
        assertThat(identities.findByIssuerAndSubject("https://issuer.test/realms/eshop", "unknown")).isEmpty();
    }
}
