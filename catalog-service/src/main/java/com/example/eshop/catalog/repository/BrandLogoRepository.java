package com.example.eshop.catalog.repository;

import com.example.eshop.catalog.model.BrandLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandLogoRepository extends JpaRepository<BrandLogo, Long> {
}
