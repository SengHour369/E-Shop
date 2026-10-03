package com.example.eshop.notification.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="notification_preferences") @Getter @Setter
public class NotificationPreference {
    @Id private Long userId;
    private boolean aiEmail;
}
