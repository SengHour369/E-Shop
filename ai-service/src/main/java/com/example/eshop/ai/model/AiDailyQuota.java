package com.example.eshop.ai.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name="ai_daily_quota") @Getter @Setter
public class AiDailyQuota {
 @Id private Long userId;
 @Column(nullable=false) private LocalDate businessDay;
 @Column(nullable=false) private int used;
}
