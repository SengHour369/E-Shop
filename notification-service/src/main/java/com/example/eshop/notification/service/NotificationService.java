package com.example.eshop.notification.service;
import com.example.eshop.notification.model.*;
import com.example.eshop.notification.repository.*;
import com.example.eshop.notification.dto.NotificationResponse;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final NotificationPreferenceRepository preferences;
    @Transactional
    public void receive(NotificationEvent e) {
        e.validate();
        if (repository.existsByEventId(e.eventId())) return;
        var n = new Notification(); n.setEventId(e.eventId()); n.setUserId(e.userId()); n.setType(e.type());
        n.setTitle(e.type()==NotificationEvent.Type.AI_ACTION_COMPLETED ? "AI action completed" : "AI action needs attention");
        n.setMessage(e.type()==NotificationEvent.Type.AI_ACTION_COMPLETED ? "Your requested action completed. View its execution for details." : "Your requested action could not be confirmed. Check its execution before trying again.");
        n.setResourceType(e.resourceType()); n.setResourceId(e.resourceId()); n.setRequestId(e.requestId()); n.setTraceId(e.traceId());
        n.setAiExecutionId(e.aiExecutionId()); n.setCreatedAt(Instant.now()); n.setSentAt(Instant.now()); n.setStatus(Notification.Status.SENT);
        boolean email = preferences.findById(e.userId()).map(NotificationPreference::isAiEmail).orElse(false);
        n.setEmailStatus(email ? Notification.EmailStatus.PENDING : Notification.EmailStatus.DISABLED);
        if (email) n.setNextEmailAttemptAt(Instant.now());
        repository.saveAndFlush(n);
    }
    @Transactional(readOnly=true)
    public Page<NotificationResponse> list(int page, int size, Notification.Status status, NotificationEvent.Type type, Boolean read, Instant from, Instant to) {
        if (page < 1 || size < 1 || size > 100 || from != null && to != null && from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters");
        long owner = CurrentActor.userId();
        Specification<Notification> spec = (root,q,cb) -> {
            var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            p.add(cb.equal(root.get("userId"),owner));
            if(status!=null) p.add(cb.equal(root.get("status"),status));
            if(type!=null) p.add(cb.equal(root.get("type"),type));
            if(read!=null) p.add(read ? cb.isNotNull(root.get("readAt")) : cb.isNull(root.get("readAt")));
            if(from!=null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));
            if(to!=null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"),to));
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(spec,PageRequest.of(page-1,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).map(NotificationResponse::from);
    }
    @Transactional(readOnly=true)
    public NotificationResponse get(long id) { return NotificationResponse.from(owned(id)); }
    private Notification owned(long id) {
        return repository.findByIdAndUserId(id,CurrentActor.userId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
    }
    @Transactional(readOnly=true)
    public long unread() { return repository.countByUserIdAndReadAtIsNull(CurrentActor.userId()); }
    @Transactional
    public void markRead(long id) { owned(id); repository.markRead(id,CurrentActor.userId(),Instant.now()); }
    @Transactional
    public int markAll() { return repository.markAll(CurrentActor.userId(),Instant.now()); }
    @Transactional
    public void preference(boolean email) {
        var p = new NotificationPreference(); p.setUserId(CurrentActor.userId()); p.setAiEmail(email); preferences.save(p);
    }
    @Transactional(readOnly=true)
    public boolean emailPreference() { return preferences.findById(CurrentActor.userId()).map(NotificationPreference::isAiEmail).orElse(false); }
}
