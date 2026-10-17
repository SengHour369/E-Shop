package com.example.eshop.auth.service.impl;

import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.exception.ResourceNotFoundException;
import com.example.eshop.auth.exception.BadRequestException;
import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.*;
import java.util.*;

@Service("adminUserDetailsService")
@RequiredArgsConstructor
public class UserDetailsService {
    private final UserEditorService editor;
    private final UserRepository users;
    private final GroupRepository groups;
    private final FunctionPermissionRepository functions;
    private final RefreshTokenRepository refreshTokens;
    private final AuditLogRepository audit;
    private User user(Long id) { return users.findUserById(id).orElseThrow(()->new ResourceNotFoundException("User not found")); }
    private String text(String value) { return value==null?"":value; }
    private Instant instant(LocalDateTime time) { return time==null?null:time.atZone(ZoneId.systemDefault()).toInstant(); }
    private Specification<AuditLog> relevant(Long id) {
        return (root,q,cb)->cb.or(cb.equal(root.get("actorId"),id.toString()),cb.and(cb.equal(root.get("resourceType"),"USER"),cb.equal(root.get("resourceId"),id.toString())));
    }
    private Instant latest(Long id, List<AuditAction> actions) {
        Specification<AuditLog> spec=relevant(id).and((root,q,cb)->cb.and(root.get("action").in(actions),cb.equal(root.get("result"),AuditResult.SUCCESS),cb.equal(root.get("resourceType"),"USER"),cb.equal(root.get("resourceId"),id.toString())));
        return audit.findAll(spec,PageRequest.of(0,1,Sort.by(Sort.Direction.DESC,"occurredAt","id"))).getContent().stream().findFirst().map(AuditLog::getOccurredAt).orElse(null);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> get(Long id) {
        User u=user(id);Map<String,Object> out=new LinkedHashMap<>(editor.get(id));
        out.put("createdAt",instant(u.getCreatedAt()));
        out.put("manager",u.getManagerId()==null?"":users.findUserById(u.getManagerId()).map(m->text(m.getFullName()).isBlank()?m.getUsername():m.getFullName()).orElse(""));
        out.put("roles",u.getRoles()==null?List.of():u.getRoles().stream().map(Role::getName).sorted().toList());
        @SuppressWarnings("unchecked") List<Long> groupIds=(List<Long>)out.get("groupIds");
        out.put("groups",groups.findAllById(groupIds).stream().filter(g->!Boolean.TRUE.equals(g.getIsDelete())).map(g->Map.of("id",g.getId(),"name",text(g.getName()),"description",text(g.getDescription()),"active",Boolean.TRUE.equals(g.getIsActive()))).toList());
        boolean admin=u.getRoles()!=null&&u.getRoles().stream().anyMatch(r->Set.of("ADMIN","SUPER_ADMIN").contains(r.getName()));
        var codes=admin?functions.activeCodes():functions.effectiveCodes(id);
        out.put("permissions",functions.findAll().stream().filter(f->codes.contains(f.getFuncCode())).sorted(Comparator.comparing(FunctionPermission::getFuncCode))
            .map(f->Map.of("code",f.getFuncCode(),"name",text(f.getFuncName()).isBlank()?f.getFuncCode():f.getFuncName())).toList());
        out.put("accessLevel",!u.isEnabled()||!"ACT".equals(u.getStatus())?"Account access blocked":admin?"Administrator":"Assigned permissions");
        out.put("lastLogin",latest(id,List.of(AuditAction.LOGIN)));
        out.put("passwordChangedAt",latest(id,List.of(AuditAction.PASSWORD_CHANGE,AuditAction.PASSWORD_RESET)));
        out.put("loginAttempts",u.getAttempt());
        var sessions=!u.isEnabled()||!"ACT".equals(u.getStatus())?List.<RefreshToken>of():refreshTokens.findByUser_IdAndExpiresAtAfterOrderByCreatedAtDesc(id,LocalDateTime.now());
        out.put("activeSessions",sessions.size());
        out.put("sessions",sessions.stream().map(s->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",s.getId());row.put("createdAt",instant(s.getCreatedAt()));row.put("expiresAt",instant(s.getExpiresAt()));return row;}).toList());
        out.put("timezone",null);
        return out;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> activity(Long id,int page) {
        user(id);if(page<0)throw new BadRequestException("Activity page must be zero or greater");
        var result=audit.findAll(relevant(id),PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"occurredAt","id")));
        var actors=new HashSet<Long>();for(var event:result){try{actors.add(Long.parseLong(event.getActorId()));}catch(Exception ignored){}}
        Map<Long,String> names=new HashMap<>();users.findAllById(actors).forEach(u->names.put(u.getId(),text(u.getFullName()).isBlank()?u.getUsername():u.getFullName()));
        var items=result.getContent().stream().map(event->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",event.getId());row.put("actorId",event.getActorId());row.put("actorName",null);try{row.put("actorName",names.get(Long.parseLong(event.getActorId())));}catch(Exception ignored){}
            row.put("action",event.getAction());row.put("resourceType",event.getResourceType());row.put("resourceId",event.getResourceId());row.put("result",event.getResult());row.put("occurredAt",event.getOccurredAt());row.put("ipAddress",event.getIpAddress());return row;}).toList();
        return Map.of("items",items,"totalElements",result.getTotalElements(),"totalPages",result.getTotalPages(),"page",page);
    }
}
