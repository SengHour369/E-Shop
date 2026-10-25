package com.example.eshop.auth.repository;
import com.example.eshop.auth.model.WorkflowGrant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkflowGrantRepository extends JpaRepository<WorkflowGrant,UUID> {}
