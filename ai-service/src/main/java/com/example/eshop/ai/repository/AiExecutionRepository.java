package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AiExecutionRepository extends JpaRepository<AiExecution,UUID> {}
