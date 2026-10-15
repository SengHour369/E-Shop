package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.client.BranchIdentityClient;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branches;
    private final BranchAccessRepository access;
    private final InventoryRepository inventories;
    private final LivePermissionService permissions;
    private final AuditLogService audit;
    private final BranchIdentityClient identities;

    private void require(String code) {
        if (!permissions.current().permits(code)) {
            throw new AccessDeniedException("Branch permission required");
        }
    }

    private void administrator() {
        var actor = permissions.current();

        if (!actor.administrator() || !actor.permits("BRANCH_ACCESS_MANAGE")) {
            throw new AccessDeniedException(
                    "Branch access administrator required"
            );
        }
    }

    public static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public boolean duplicate(String name) {
        return branches.existsByNameKey(key(name));
    }

    public void scope(Long branchId) {
        if (branchId != null) {
            scope(branchId, permissions.current());
        }
    }

    public void scope(Long branchId, PermissionSnapshot actor) {
        if (branchId == null) {
            // Existing unassigned inventory remains global; never infer a branch from warehouse text.
            return;
        }

        Branch b = branches.findById(branchId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "UNKNOWN_BRANCH"
                ));

        if (!actor.administrator()
                && !access.existsByBranchIdAndUserIdAndActiveTrue(
                b.getId(),
                actor.userId()
        )) {
            throw new AccessDeniedException("Branch access required");
        }
    }

    @Transactional(readOnly = true)
    public Page<Branch> list(int page) {
        require("BRANCH_VIEW");
        var pageable = PageRequest.of(
                page,
                25,
                Sort.by("id").descending()
        );
        var actor = permissions.current();
        return actor.administrator()
                ? branches.findAll(pageable)
                : branches.visible(actor.userId(), pageable);
    }

    @Transactional
    public Branch create(String name, String address) {
        require("BRANCH_CREATE");

        if (name == null
                || name.isBlank()
                || name.length() > 120
                || address != null && address.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid branch details"
            );
        }

        if (duplicate(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_BRANCH"
            );
        }

        Branch b = new Branch();
        b.setName(name.trim());
        b.setNameKey(key(name));
        b.setAddress(address);
        branches.saveAndFlush(b);
        audit.record(
                AuditEvent.success(
                        AuditAction.CREATE,
                        "BRANCH",
                        b.getId(),
                        null,
                        Map.of("name", b.getName())
                )
        );
        return b;
    }

    @Transactional(readOnly = true)
    public List<BranchAccess> members(long branchId) {
        administrator();
        return access.findByBranchId(branchId);
    }

    @Transactional
    public BranchAccess grant(long branchId, long userId, boolean active) {
        administrator();

        if (userId < 1 || !branches.existsById(branchId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown branch or user"
            );
        }

        var attributes = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
        String token = attributes instanceof org.springframework.web.context.request.ServletRequestAttributes r
                ? r.getRequest().getHeader("Authorization")
                : null;

        if (active && (token == null || !identities.active(token, userId))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Active user required"
            );
        }

        BranchAccess a = access.findByBranchIdAndUserId(branchId, userId)
                .orElseGet(BranchAccess::new);
        a.setBranchId(branchId);
        a.setUserId(userId);
        a.setActive(active);
        a.setUpdatedBy(CurrentActor.userId());
        a.setUpdatedAt(Instant.now());
        access.saveAndFlush(a);
        audit.record(
                AuditEvent.success(
                        active
                                ? AuditAction.ROLE_GRANTED
                                : AuditAction.ROLE_REVOKED,
                        "BRANCH_ACCESS",
                        a.getId(),
                        null,
                        Map.of(
                                "branchId", branchId,
                                "userId", userId,
                                "active", active
                        )
                )
        );
        return a;
    }

    @Transactional
    public void assignInventory(long branchId, long inventoryId) {
        administrator();
        require("INVENTORY_ADJUST");
        scope(branchId);
        Inventory i = inventories.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Unknown inventory"
                ));
        Long before = i.getBranchId();
        i.setBranchId(branchId);
        inventories.saveAndFlush(i);
        audit.record(
                AuditEvent.success(
                        AuditAction.UPDATE,
                        "INVENTORY_BRANCH",
                        inventoryId,
                        before == null ? null : Map.of("branchId", before),
                        Map.of("branchId", branchId)
                )
        );
    }
}