package com.example.eshop.common.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="automation.enabled", havingValue="true")
public class AutomationWorker {
    private final AutomationEvents events;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final KafkaTemplate<String,String> kafka;
    private final TransactionTemplate transaction;
    private final CommerceHandler handler;
    @KafkaListener(topics="${automation.input-topic}",groupId="${spring.application.name}-commerce-v1")
    public void receive(String payload, @org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        if(payload==null || payload.length()>8192) throw new IllegalArgumentException("Invalid event size");
        events.receive(topic,json.readValue(payload,CommerceEvent.class));
    }
    @Scheduled(fixedDelayString="${automation.delay-ms:2000}")
    public void tick() {
        for(int i=0;i<20;i++) {
            java.util.concurrent.atomic.AtomicReference<Map<String,Object>> active=new java.util.concurrent.atomic.AtomicReference<>();
            try {
                Boolean worked=transaction.execute(tx -> {
                    var rows=jdbc.queryForList("SELECT * FROM automation_events WHERE state='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED");
                    if(rows.isEmpty()) return false;
                    var row=rows.get(0); active.set(row);
                    var previous=MDC.getCopyOfContextMap();
                    try {
                        MDC.clear(); MDC.put("requestId",row.get("request_id").toString());
                        if(row.get("trace_id")!=null) MDC.put("traceId",row.get("trace_id").toString());
                        MDC.put("eventId",row.get("event_id").toString());
                        if("OUTBOX".equals(row.get("direction")))
                            kafka.send(row.get("topic").toString(),row.get("order_id").toString(),row.get("payload").toString()).get(5,TimeUnit.SECONDS);
                        else handler.handle(json.readValue(row.get("payload").toString(),CommerceEvent.class));
                        jdbc.update("UPDATE automation_events SET state='DONE',completed_at=CURRENT_TIMESTAMP,last_error=NULL WHERE event_id=? AND direction=?",row.get("event_id"),row.get("direction"));
                        logAttempt(row,"DONE",null); return true;
                    } catch(InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted",ex); }
                    catch(Exception ex) { throw new IllegalStateException("Processing deferred",ex); }
                    finally { MDC.clear(); if(previous!=null) MDC.setContextMap(previous); }
                });
                if(!Boolean.TRUE.equals(worked)) break;
            } catch(Exception ex) {
                if(active.get()!=null) fail(active.get(),ex.getCause()==null?ex:ex.getCause());
                else break;
            }
        }
        publishDead();
    }
    private void fail(Map<String,Object> row,Throwable ex) {
        transaction.executeWithoutResult(tx -> {
            // Lock/recheck because another instance may complete a retry after our rollback.
            var rows=jdbc.queryForList("SELECT * FROM automation_events WHERE event_id=? AND direction=? FOR UPDATE",row.get("event_id"),row.get("direction"));
            if(rows.isEmpty() || !"PENDING".equals(rows.get(0).get("state"))) return;
            int attempts=((Number)rows.get(0).get("attempts")).intValue()+1;
            String state=attempts>=10?"DEAD":"PENDING";
            jdbc.update("UPDATE automation_events SET attempts=?,state=?,last_error=?,next_attempt_at=? WHERE event_id=? AND direction=?",
                    attempts,state,ex.getClass().getSimpleName(),java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(Math.min(300,5L*(1L<<Math.min(attempts,6))))),row.get("event_id"),row.get("direction"));
            logAttempt(row,state,ex.getClass().getSimpleName());
            if("DEAD".equals(state)) events.review(((Number)row.get("order_id")).longValue(),"EVENT_RETRIES_EXHAUSTED","Processing requires review; inspect event and service status before retrying.");
        });
    }
    private void logAttempt(Map<String,Object> row,String state,String reason) {
        jdbc.update("INSERT INTO automation_attempts(event_id,direction,outcome,reason,request_id) VALUES(?,?,?,?,?)",row.get("event_id"),row.get("direction"),state,reason,row.get("request_id"));
    }
    private void publishDead() {
        transaction.executeWithoutResult(tx -> {
            var rows=jdbc.queryForList("SELECT * FROM automation_events WHERE state='DEAD' AND dead_published=FALSE ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED");
            for(var row:rows) try {
                kafka.send(row.get("topic")+".DLT",row.get("order_id").toString(),row.get("payload").toString()).get(5,TimeUnit.SECONDS);
                jdbc.update("UPDATE automation_events SET dead_published=TRUE WHERE event_id=? AND direction=?",row.get("event_id"),row.get("direction"));
            } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
              catch(Exception ignored) { /* Retained durably and retried next tick. */ }
        });
    }
}
