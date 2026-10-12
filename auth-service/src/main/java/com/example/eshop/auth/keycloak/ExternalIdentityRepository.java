package com.example.eshop.auth.keycloak;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByIssuerAndSubject(String issuer, String subject);
}
