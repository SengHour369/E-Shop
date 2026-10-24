 package com.example.eshop.ai.model;

 import jakarta.persistence.*;
 import lombok.Getter;
 import lombok.Setter;
 import java.time.LocalDate;

 @Entity
 @Table(name = "ai_daily_quota")
 @Getter
 @Setter
 public class AiDailyQuota implements org.springframework.data.domain.Persistable<Long> {

  @Id
  private Long userId;

  @Column(nullable = false)
  private LocalDate businessDay;

  @Column(nullable = false)
  private int used;

  // Assigned IDs must INSERT on first use. merge() can overwrite a concurrent user's counter.
  @Transient
  private boolean newRow = true;

  @Override
  public Long getId() {
   return userId;
  }

  @Override
  public boolean isNew() {
   return newRow;
  }

  @PostLoad
  @PostPersist
  void persisted() {
   newRow = false;
  }
 }