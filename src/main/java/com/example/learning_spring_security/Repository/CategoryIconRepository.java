package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.CategoryIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryIconRepository extends JpaRepository<CategoryIcon, Long> {
}