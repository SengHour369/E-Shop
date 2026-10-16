package com.example.eshop.payment;

import com.example.eshop.common.automation.*;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.payment.automation.*;
import com.example.eshop.payment.bakong.service.BakongService;
import com.example.eshop.payment.repository.*;
import com.example.eshop.payment.service.PaymentTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiKhqrOwnershipTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void adminCustomerChatCannotRetrieveAnotherCustomersQr(){
        var authentication=new UsernamePasswordAuthenticationToken("admin",null,List.of(new SimpleGrantedAuthority("ADMIN")));
        authentication.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(authentication);
        var orders=mock(OrderAutomationClient.class);var transactions=mock(PaymentTransactionRepository.class);
        when(orders.get(3)).thenReturn(new PaymentOrderSnapshot(3,8,BigDecimal.TEN,"USD","PENDING_PAYMENT","KHQR"));
        var service=new PaymentAutomation(transactions,mock(PaymentTransactionStatusHistoryRepository.class),mock(PaymentRepository.class),
            mock(PaymentTransactionService.class),orders,mock(BakongService.class),new ObjectMapper(),mock(AutomationEvents.class),mock(AuditLogService.class),mock(TransactionTemplate.class));
        assertThatThrownBy(()->service.customerQr(3)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(transactions);
    }
}
