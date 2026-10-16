package com.example.eshop.payment.automation;
import com.example.eshop.common.automation.AutomationEvents;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.payment.bakong.service.BakongService;
import com.example.eshop.payment.bakong.dto.BakongResponse;
import com.example.eshop.payment.model.PaymentTransaction;
import com.example.eshop.payment.repository.*;
import com.example.eshop.payment.service.PaymentTransactionService;
import com.example.eshop.payment.enumeration.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class PaymentAutomationTest {
    PaymentTransactionRepository repository=mock(PaymentTransactionRepository.class);BakongService bakong=mock(BakongService.class);
    AutomationEvents events=mock(AutomationEvents.class);TransactionTemplate tx=mock(TransactionTemplate.class);PaymentAutomation service;PaymentTransaction record;
    @BeforeEach void setup(){
      service=new PaymentAutomation(repository,mock(PaymentTransactionStatusHistoryRepository.class),mock(PaymentRepository.class),mock(PaymentTransactionService.class),mock(OrderAutomationClient.class),bakong,new ObjectMapper(),events,mock(AuditLogService.class),tx);
      record=new PaymentTransaction();record.setId(3L);record.setOrder(1L);record.setCustomer(2L);record.setAmount(BigDecimal.TEN);record.setCurrency("USD");record.setStatus(TransactionStatus.PENDING);record.setQrMd5("b".repeat(32));record.setRecipientAccount("merchant@bank");record.setExpiresAt(Instant.now().minusSeconds(120));
      when(repository.findById(3L)).thenReturn(Optional.of(record));when(repository.lockById(3L)).thenReturn(Optional.of(record));
      doAnswer(i->{java.util.function.Consumer<org.springframework.transaction.TransactionStatus> c=i.getArgument(0);c.accept(new SimpleTransactionStatus());return null;}).when(tx).executeWithoutResult(any());
    }
    @Test void individualModeAcceptsAccountWithoutMerchantDetailsButMerchantModeStillRejects(){
      org.springframework.test.util.ReflectionTestUtils.setField(service,"recipient","senghour_soeurng@bkrt");
      org.springframework.test.util.ReflectionTestUtils.setField(service,"merchantName","");
      org.springframework.test.util.ReflectionTestUtils.setField(service,"merchantId","");
      org.springframework.test.util.ReflectionTestUtils.setField(service,"acquiringBank","");
      org.springframework.test.util.ReflectionTestUtils.setField(service,"khqrType",com.example.eshop.payment.bakong.KhqrType.INDIVIDUAL);
      assertThatCode(service::validateMerchantConfiguration).doesNotThrowAnyException();
      org.springframework.test.util.ReflectionTestUtils.setField(service,"khqrType",com.example.eshop.payment.bakong.KhqrType.MERCHANT);
      assertThatThrownBy(service::validateMerchantConfiguration).isInstanceOf(IllegalStateException.class);
      verifyNoInteractions(bakong,events);
    }
    @Test void unavailableProviderCannotReleaseOrComplete(){when(bakong.checkTransactionByMD5(any())).thenThrow(new IllegalStateException("unavailable"));assertThatThrownBy(()->service.verify(3)).isInstanceOf(IllegalStateException.class);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void wrongRecipientCannotComplete(){when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(0).data(Map.of("hash","a".repeat(64),"amount",10,"currency","USD","toAccountId","attacker@bank")).build());assertThatThrownBy(()->service.verify(3)).isInstanceOf(IllegalStateException.class);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void authoritativeNotFoundAfterExpiryEmitsFailure(){when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);assertThat(record.getStatus()).isEqualTo(TransactionStatus.FAILED);verify(events).emit(anyString(),argThat(e->e.type()==com.example.eshop.common.automation.CommerceEvent.Type.PAYMENT_EXPIRED));}
    @Test void notFoundBeforeExpiryStaysPending(){record.setExpiresAt(Instant.now().plusSeconds(900));when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void repeatedExpiryDoesNotRepublish(){record.setStatus(TransactionStatus.FAILED);when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);verifyNoInteractions(events);}
}
