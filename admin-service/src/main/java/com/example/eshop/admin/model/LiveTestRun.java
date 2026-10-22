package com.example.eshop.admin.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="live_test_runs") @Getter @Setter
public class LiveTestRun {
 @Id private UUID id;
 @Column(nullable=false) private long actorId;
 @Column(nullable=false) private String status;
 @Column(nullable=false) private Instant startedAt;
 private Instant finishedAt;
 @Column(nullable=false,columnDefinition="text") private String stepsJson;
}
