package com.example.eshop.auth.controller;

import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.model.WorkflowGrant;
import com.example.eshop.auth.service.AccountAccessService;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import io.jsonwebtoken.Jwts;

@RestController @RequiredArgsConstructor
public class WorkflowGrantController {
 private final WorkflowGrantRepository grants;private final UserRepository users;
 private final AccountAccessService access;private final JwtService jwt;private final AuditLogService audit;
 public record Create(UUID versionId,Instant expiresAt){}
 public record Exchange(UUID grantId,UUID versionId){}
 @PostMapping("/api/v1/user/workflow-grants") @Transactional
 public WorkflowGrant create(@RequestBody Create body){
  long owner=CurrentActor.userId();var user=users.findById(owner).orElseThrow(()->new AccessDeniedException("Account unavailable"));
  if(!access.snapshot(user).administrator())throw new AccessDeniedException("Administrator required");
  var now=Instant.now();if(body.versionId()==null||body.expiresAt()==null||!body.expiresAt().isAfter(now)||body.expiresAt().isAfter(now.plusSeconds(30L*86400)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Authorization must expire within 30 days");
  var grant=new WorkflowGrant();grant.setId(UUID.randomUUID());grant.setOwnerId(owner);grant.setVersionId(body.versionId());grant.setExpiresAt(body.expiresAt());grant.setCreatedAt(now);grants.save(grant);
  audit.record(AuditEvent.success(AuditAction.CREATE,"WORKFLOW_AUTHORIZATION",grant.getId(),null,Map.of("versionId",body.versionId().toString(),"expiresAt",body.expiresAt().toString())));return grant;
 }
 @DeleteMapping("/api/v1/user/workflow-grants/{id}") @Transactional
 public void revoke(@PathVariable UUID id){var grant=grants.findById(id).filter(g->g.getOwnerId()==CurrentActor.userId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Authorization not found"));grant.setRevoked(true);audit.record(AuditEvent.success(AuditAction.UPDATE,"WORKFLOW_AUTHORIZATION",id,null,Map.of("revoked",true)));}
 @PostMapping("/internal/workflow-authorization/token") @Transactional(readOnly=true)
 public Map<String,Object> exchange(@RequestBody Exchange body){
  var grant=grants.findById(body.grantId()).filter(g->!g.isRevoked()&&g.getExpiresAt().isAfter(Instant.now())&&g.getVersionId().equals(body.versionId())).orElseThrow(()->new AccessDeniedException("Workflow authorization expired or revoked"));
  var user=users.findById(grant.getOwnerId()).orElseThrow(()->new AccessDeniedException("Account unavailable"));if(!access.snapshot(user).administrator())throw new AccessDeniedException("Administrator required");
  var now=Instant.now();var expiry=now.plusSeconds(600).isBefore(grant.getExpiresAt())?now.plusSeconds(600):grant.getExpiresAt();
  String token=Jwts.builder().setSubject(user.getUsername()).claim("userId",user.getId()).claim("authorities",List.of("WORKFLOW_READ")).claim("isEnable",true).claim("workflowGrantId",grant.getId().toString()).claim("workflowVersionId",grant.getVersionId().toString()).setIssuedAt(Date.from(now)).setExpiration(Date.from(expiry)).signWith(jwt.getKey()).compact();
  return Map.of("ownerId",user.getId(),"username",user.getUsername(),"accessToken",token);
 }
}
