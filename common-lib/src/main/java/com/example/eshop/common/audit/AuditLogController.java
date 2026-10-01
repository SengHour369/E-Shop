package com.example.eshop.common.audit;

import com.example.eshop.common.dto.APIResponse;
import com.example.eshop.common.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {
    private final AuditLogRepository repository;
    @GetMapping @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDIT_READ')")
    @Transactional(readOnly = true)
    public APIResponse<Page<AuditLog>> search(@ModelAttribute AuditSearch filters,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        if (page < 1 || size < 1 || size > 100 || !java.util.Set.of("id", "occurredAt").contains(sort)
                || (filters.from() != null && filters.to() != null && filters.from().isAfter(filters.to())))
            throw new BusinessLogicException("Invalid audit pagination, sort or time range");
        var ordering = Sort.by(direction, sort);
        if (!sort.equals("id")) ordering = ordering.and(Sort.by(direction, "id"));
        return APIResponse.success("Audit logs retrieved", 200,
                repository.findAll(filters.specification(), PageRequest.of(page - 1, size, ordering)));
    }
}
