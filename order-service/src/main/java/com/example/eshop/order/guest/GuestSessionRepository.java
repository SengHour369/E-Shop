package com.example.eshop.order.guest;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
public interface GuestSessionRepository extends JpaRepository<GuestSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GuestSession g where g.id=:id")
    Optional<GuestSession> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GuestSession> findByTokenHash(String hash);
}
