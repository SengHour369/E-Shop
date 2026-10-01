package com.example.eshop.catalog.repository;

import com.example.eshop.catalog.model.CategoryIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryIconRepository extends JpaRepository<CategoryIcon, Long> {
}