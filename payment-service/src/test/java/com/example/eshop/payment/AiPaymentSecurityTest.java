package com.example.eshop.payment;

import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.payment.controller.AiPaymentController;
import com.example.eshop.payment.model.Payment;
import com.example.eshop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiPaymentSecurityTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void privatePaymentLookupAlwaysIncludesCurrentUser() {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(Map.of("userId", 7L));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var repository = mock(PaymentRepository.class);
        var permissions = mock(LivePermissionService.class);
        var controller = new AiPaymentController(repository, permissions);
        when(repository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.mine(99)).hasMessage("Payment not found");
        verify(repository).findByIdAndUserId(99L, 7L);

        var payment = new Payment();
        payment.setId(99L);
        payment.setStatus("FAILED");
        payment.setPaymentProviderResponse("sensitive gateway payload");
        when(repository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.of(payment));
        assertThat(controller.mine(99).toString()).doesNotContain("sensitive gateway payload");
    }

    @Test
    void adminQueryCannotRunWithoutFunctionPermission() {
        var repository = mock(PaymentRepository.class);
        var permissions = mock(LivePermissionService.class);
        doThrow(new AccessDeniedException("denied")).when(permissions).require("PAYMENT_VIEW_ALL");
        assertThatThrownBy(() -> new AiPaymentController(repository, permissions).admin("FAILED", null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void revenuePermissionIsCheckedBeforeAggregation() {
        var repository = mock(PaymentRepository.class);
        var permissions = mock(LivePermissionService.class);
        doThrow(new AccessDeniedException("denied")).when(permissions).require("REPORT_VIEW");
        assertThatThrownBy(() -> new AiPaymentController(repository, permissions)
                .revenue(java.time.LocalDate.of(2026, 10, 9)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }
}
