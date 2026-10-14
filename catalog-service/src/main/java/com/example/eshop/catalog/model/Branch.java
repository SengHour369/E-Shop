package com.example.eshop.catalog.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="catalog_branch") @Getter @Setter
public class Branch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false,length=120) private String name;
 @Column(nullable=false,unique=true,length=120) private String nameKey;
 @Column(length=500) private String address;
 @Column(nullable=false) private boolean active=true;
 @Column(nullable=false) private Instant createdAt=Instant.now();
}
