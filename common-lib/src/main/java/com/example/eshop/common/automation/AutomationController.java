package com.example.eshop.common.automation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequiredArgsConstructor
@RequestMapping("${automation.api-path:/api/v1/orders/automation}")
@ConditionalOnProperty(name="automation.enabled",havingValue="true")
@PreAuthorize("hasAuthority('ADMIN')")
public class AutomationController {
    private final JdbcTemplate jdbc;
    @GetMapping("/events") public List<Map<String,Object>> events(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Long orderId,@RequestParam(required=false) String state) {
        if(page<0 || page>100000 || state!=null && !Set.of("PENDING","DONE","DEAD").contains(state)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters");
        return jdbc.queryForList("SELECT event_id,direction,topic,order_id,event_type,state,attempts,request_id,trace_id,last_error,created_at,completed_at FROM automation_events WHERE (CAST(? AS BIGINT) IS NULL OR order_id=?) AND (CAST(? AS VARCHAR) IS NULL OR state=?) ORDER BY created_at DESC LIMIT 50 OFFSET ?",orderId,orderId,state,state,page*50);
    }
    @GetMapping("/events/{id}/logs") public List<Map<String,Object>> logs(@PathVariable UUID id) {
        return jdbc.queryForList("SELECT * FROM automation_attempts WHERE event_id=? ORDER BY created_at DESC LIMIT 100",id);
    }
    @GetMapping("/reviews") public List<Map<String,Object>> reviews() {
        return jdbc.queryForList("SELECT * FROM automation_reviews ORDER BY created_at DESC LIMIT 100");
    }
    @PostMapping("/events/{id}/retry") @Transactional
    public Map<String,Object> retry(@PathVariable UUID id,@RequestParam String direction) {
        if(!Set.of("INBOX","OUTBOX").contains(direction)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid direction");
        int count=jdbc.update("UPDATE automation_events SET state='PENDING',attempts=0,dead_published=FALSE,next_attempt_at=CURRENT_TIMESTAMP WHERE event_id=? AND direction=? AND state='DEAD'",id,direction);
        if(count!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only dead events can be retried");
        jdbc.update("INSERT INTO automation_attempts(event_id,direction,outcome,reason,request_id) VALUES(?,?,?,?,?)",id,direction,"RETRY","ADMIN_RETRY:"+com.example.eshop.common.security.CurrentActor.userId(),com.example.eshop.common.request.RequestIds.current());
        return Map.of("eventId",id,"state","PENDING");
    }
}
