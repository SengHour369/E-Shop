package com.example.eshop.admin.service;

import com.example.eshop.admin.model.StorefrontAccessSettings;
import com.example.eshop.admin.repository.StorefrontAccessSettingsRepository;
import com.example.eshop.common.storefront.StorefrontAccess;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StorefrontAccessService {

    private final StorefrontAccessSettingsRepository repository;
    private final LivePermissionService permissions;
    private final AuditLogService audit;

    @Transactional(readOnly = true)
    public StorefrontAccess current() {
        return repository.findById(1L)
                .map(s -> new StorefrontAccess(
                        s.isGuestBrowsing(),
                        s.isGuestReviews(),
                        s.isGuestCart(),
                        s.isGuestCheckout(),
                        s.isGuestPayment() && s.isGuestCheckout(),
                        true,
                        s.isRequireGuestContactVerification(),
                        s.isGuestOrderTracking()
                ))
                .orElse(StorefrontAccess.defaults());
    }

    @Transactional
    public StorefrontAccess save(StorefrontAccess request) {
        permissions.require("STOREFRONT_ACCESS_UPDATE");

        if (request.guestPayment() && !request.guestCheckout()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guest payment requires guest checkout"
            );
        }

        if (request.guestCheckout() && !request.guestCart()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guest checkout requires guest cart"
            );
        }

        if (!request.requireDeliveryAddress()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery address is mandatory for physical products"
            );
        }

        var s = repository.findById(1L)
                .orElseGet(StorefrontAccessSettings::new);
        s.setGuestBrowsing(request.guestBrowsing());
        s.setGuestReviews(request.guestReviews());
        s.setGuestCart(request.guestCart());
        s.setGuestCheckout(request.guestCheckout());
        s.setGuestPayment(request.guestPayment());
        s.setRequireDeliveryAddress(true);
        s.setRequireGuestContactVerification(
                request.requireGuestContactVerification()
        );
        s.setGuestOrderTracking(request.guestOrderTracking());
        s.setUpdatedAt(Instant.now());
        s.setUpdatedBy(CurrentActor.userId());
        repository.saveAndFlush(s);
        audit.record(
                new AuditEvent(
                        AuditAction.UPDATE,
                        "STOREFRONT_ACCESS",
                        "1",
                        null,
                        Map.of(
                                "guestCheckout", request.guestCheckout(),
                                "guestPayment", request.guestPayment(),
                                "guestReviews", request.guestReviews()
                        ),
                        AuditResult.SUCCESS,
                        null
                )
        );
        return current();
    }
}