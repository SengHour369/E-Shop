package com.example.eshop.order.guest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GuestOrderAccessRepository extends JpaRepository<GuestOrderAccess,UUID> {
    Optional<GuestOrderAccess> findByOrderIdAndGuestSessionId(Long orderId,UUID sessionId);
}
