package com.example.eshop.auth.service;

import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserApprovalRouting {
    private final UserRepository users;
    private final AccountAccessService access;
    public boolean eligible(User user){
        return user!=null && user.isEnabled() && "ACT".equals(user.getStatus()) && !Boolean.TRUE.equals(user.getDeleted())
            && access.snapshot(user).permissions().containsAll(Set.of("IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE"));
    }
    public List<Map<String,Object>> candidates(){
        return users.findAllUser().stream().filter(this::eligible).map(user->{
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",user.getId());row.put("name",user.getFullName()==null||user.getFullName().isBlank()?user.getUsername():user.getFullName());row.put("username",user.getUsername());return row;
        }).toList();
    }
    @Transactional(readOnly=true)
    public Map<String,Object> currentOptions(){
        var actor=users.findById(CurrentActor.userId()).orElseThrow(()->new org.springframework.security.access.AccessDeniedException("Account unavailable"));
        if(!access.snapshot(actor).permits("IMPORT_UPLOAD"))throw new org.springframework.security.access.AccessDeniedException("Import permission required");
        var candidates=candidates().stream().filter(row->!Objects.equals(row.get("id"),actor.getId())).toList();
        Long selected=actor.getApprovalReviewerId();
        Map<String,Object> result=new LinkedHashMap<>();result.put("reviewers",candidates);
        result.put("defaultReviewerId",candidates.stream().anyMatch(row->Objects.equals(row.get("id"),selected))?selected:null);return result;
    }
}
