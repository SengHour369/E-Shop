package com.example.eshop.auth.repository;
import com.example.eshop.auth.model.NotificationMailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface NotificationMailDeliveryRepository extends JpaRepository<NotificationMailDelivery,UUID> {}
