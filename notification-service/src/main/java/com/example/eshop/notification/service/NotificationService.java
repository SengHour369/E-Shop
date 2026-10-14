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
        n.setTitle(switch(e.type()) {
            case ORDER_CREATED -> "Order received";
            case ORDER_UPDATED -> "Order updated";
            case ORDER_CANCELLED -> "Order cancelled";
            case PAYMENT_SUCCESS -> "Payment verified";
            case PAYMENT_FAILED -> "Payment not completed";
            case AI_ACTION_COMPLETED -> "AI action completed";
            case AI_ACTION_FAILED -> "AI action needs attention";
            case IMPORT_SUBMITTED -> "Excel import awaiting review";
            case IMPORT_CHANGES_REQUESTED -> "Excel import changes requested";
            case IMPORT_APPROVED -> "Excel import approved";
            case IMPORT_REJECTED -> "Excel import rejected";
            case IMPORT_PROCESSING -> "Excel import processing";
            case IMPORT_COMPLETED -> "Excel import completed";
            case IMPORT_PARTIAL_SUCCESS -> "Excel import partially completed";
            case IMPORT_FAILED -> "Excel import needs attention";
        });
        n.setMessage(switch(e.type()) {
            case PAYMENT_SUCCESS -> "Your payment was verified with the provider. View the order for its current status.";
            case PAYMENT_FAILED -> "Payment was not confirmed before the QR expired. View the order before trying again.";
            case ORDER_CREATED -> "Your order was received. Complete payment using the order's current payment instructions.";
            case ORDER_CANCELLED -> "The order was cancelled. Review the order for details.";
            case ORDER_UPDATED -> "The order status changed. View the order for details.";
            case AI_ACTION_COMPLETED -> "Your requested action completed. View its execution for details.";
            case AI_ACTION_FAILED -> "Your requested action could not be confirmed. Check its execution before trying again.";
            case IMPORT_SUBMITTED, IMPORT_CHANGES_REQUESTED, IMPORT_APPROVED, IMPORT_REJECTED,
                 IMPORT_PROCESSING, IMPORT_COMPLETED, IMPORT_PARTIAL_SUCCESS, IMPORT_FAILED -> "View the Excel import for its current review status and confirmed row results.";
        });
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
