package com.example.eshop.ai.workflow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkflowReceivedEventRepository extends JpaRepository<WorkflowReceivedEvent,UUID>{}
