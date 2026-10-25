package com.example.eshop.ai.workflow;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;
@FeignClient(name="auth-service",contextId="workflowAuthorization")
public interface WorkflowAuthorizationClient {
 record Grant(UUID id,long ownerId,UUID versionId,Instant expiresAt,boolean revoked,Instant createdAt){}
 record Create(UUID versionId,Instant expiresAt){}
 record Exchange(UUID grantId,UUID versionId){}
 record Credential(long ownerId,String username,String accessToken){}
 @PostMapping("/api/v1/user/workflow-grants") Grant create(@RequestHeader("Authorization")String bearer,@RequestBody Create body);
 @DeleteMapping("/api/v1/user/workflow-grants/{id}") void revoke(@RequestHeader("Authorization")String bearer,@PathVariable("id")UUID id);
 @PostMapping("/internal/workflow-authorization/token") Credential exchange(@RequestHeader("Authorization")String service,@RequestBody Exchange body);
}
