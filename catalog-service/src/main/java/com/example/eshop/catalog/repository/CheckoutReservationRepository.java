package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.CheckoutReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CheckoutReservationRepository extends JpaRepository<CheckoutReservation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CheckoutReservation r where r.orderId = :id")
    Optional<CheckoutReservation> lockById(@Param("id") Long id);
}

