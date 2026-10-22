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
    OrderAutomationClient orders=mock(OrderAutomationClient.class);
    PaymentTransactionRepository repository=mock(PaymentTransactionRepository.class);BakongService bakong=mock(BakongService.class);
    PaymentTransactionService transactionService=mock(PaymentTransactionService.class);
    AutomationEvents events=mock(AutomationEvents.class);TransactionTemplate tx=mock(TransactionTemplate.class);PaymentAutomation service;PaymentTransaction record;
    @BeforeEach void setup(){
      service=new PaymentAutomation(mock(com.example.eshop.payment.util.PaymentCodeGenerator.class),repository,mock(PaymentTransactionStatusHistoryRepository.class),mock(PaymentRepository.class),transactionService,orders,bakong,new ObjectMapper(),events,mock(AuditLogService.class),tx);
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
    @Test void verifiedPaymentReceivesReadableCodeAndReplayCannotGenerateAnother() {
      var codes=mock(com.example.eshop.payment.util.PaymentCodeGenerator.class);
      when(codes.generatePaymentCode()).thenReturn("anPayment_001");
      var payments=mock(PaymentRepository.class);
      when(payments.findByOrderId(1L)).thenReturn(Optional.empty());
      service=new PaymentAutomation(codes,repository,mock(PaymentTransactionStatusHistoryRepository.class),payments,transactionService,orders,bakong,new ObjectMapper(),events,mock(AuditLogService.class),tx);
      when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(0).data(Map.of("hash","a".repeat(64),"currency","USD","toAccountId","merchant@bank","amount","10.00")).build());
      service.verify(3);
      var saved=org.mockito.ArgumentCaptor.forClass(com.example.eshop.payment.model.Payment.class);
      verify(payments).saveAndFlush(saved.capture());assertThat(saved.getValue().getCode()).isEqualTo("anPayment_001");assertThat(saved.getValue().getTransactionId()).isEqualTo("a".repeat(64));
      service.verify(3);verify(codes,times(1)).generatePaymentCode();verify(payments,times(1)).saveAndFlush(any());
    }
    @Test void unavailableProviderCannotReleaseOrComplete(){when(bakong.checkTransactionByMD5(any())).thenThrow(new IllegalStateException("unavailable"));assertThatThrownBy(()->service.verify(3)).isInstanceOf(IllegalStateException.class);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void wrongRecipientCannotComplete(){when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(0).data(Map.of("hash","a".repeat(64),"amount",10,"currency","USD","toAccountId","attacker@bank")).build());assertThatThrownBy(()->service.verify(3)).isInstanceOf(IllegalStateException.class);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void authoritativeNotFoundAfterExpiryEmitsFailure(){when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);assertThat(record.getStatus()).isEqualTo(TransactionStatus.FAILED);verify(events).emit(anyString(),argThat(e->e.type()==com.example.eshop.common.automation.CommerceEvent.Type.PAYMENT_EXPIRED));}
    @Test void notFoundBeforeExpiryStaysPending(){record.setExpiresAt(Instant.now().plusSeconds(900));when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);assertThat(record.getStatus()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);}
    @Test void repeatedExpiryDoesNotRepublish(){record.setStatus(TransactionStatus.FAILED);when(bakong.checkTransactionByMD5(any())).thenReturn(BakongResponse.builder().responseCode(1).errorCode(1).build());service.verify(3);verifyNoInteractions(events);}
    @AfterEach void clearIdentity(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    void ownedOrder(){
      when(orders.get(1)).thenReturn(new com.example.eshop.common.automation.PaymentOrderSnapshot(1,2,BigDecimal.TEN,"USD","PENDING","BAKONG"));
      when(repository.findByOrder(1L)).thenReturn(List.of(record));
      var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("customer","unused",List.of());auth.setDetails(Map.of("userId",2));org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
      record.setQrPayload("qr");record.setTransactionNo("PAY-TEST");
    }
    @Test void existingSuccessfulTransactionNeverCreatesAnotherPayment(){
      ownedOrder();record.setStatus(TransactionStatus.SUCCESS);record.setQrMd5(null);
      service.handle(new com.example.eshop.common.automation.CommerceEvent(1,UUID.randomUUID(),com.example.eshop.common.automation.CommerceEvent.Type.ORDER_CREATED,1,2,BigDecimal.TEN,"USD","BAKONG",null,"test",null,Instant.now()));
      verifyNoInteractions(bakong);verify(events).review(eq(1L),eq("EXISTING_PAYMENT_REQUIRES_REVIEW"),anyString());
    }
    @Test void unverifiedSuccessCannotBeDisplayedAsPaymentVerified(){ownedOrder();record.setStatus(TransactionStatus.SUCCESS);record.setQrMd5(null);var result=service.customerQr(1);assertThat(result.get("status")).isEqualTo("REVIEW_REQUIRED");assertThat(result.get("paymentVerified")).isEqualTo(false);assertThat(result).doesNotContainKey("paidAmount");}
    @Test void receiptShowsOnlyOwnedProviderVerifiedAmount(){ownedOrder();record.setStatus(TransactionStatus.SUCCESS);record.setVerifiedAt(Instant.now());record.setProviderHash("a".repeat(64));var result=service.customerQr(1);assertThat(result.get("paidAmount")).isEqualTo(BigDecimal.TEN);assertThat(result.get("transactionReference")).isEqualTo("PAY-TEST");assertThat(result.get("paymentVerified")).isEqualTo(true);record.setAmount(BigDecimal.ONE);assertThatThrownBy(()->service.customerQr(1)).isInstanceOf(RuntimeException.class);}

    @Test void existingVerifiedTransactionReconcilesOrderWithoutNewQr(){ownedOrder();record.setStatus(TransactionStatus.SUCCESS);record.setVerifiedAt(Instant.now());record.setProviderHash("a".repeat(64));service.handle(new com.example.eshop.common.automation.CommerceEvent(1,UUID.randomUUID(),com.example.eshop.common.automation.CommerceEvent.Type.ORDER_CREATED,1,2,BigDecimal.TEN,"USD","BAKONG",null,"test",null,Instant.now()));verifyNoInteractions(bakong);verify(events).emit(anyString(),argThat(e->e.type()==com.example.eshop.common.automation.CommerceEvent.Type.PAYMENT_VERIFIED&&e.paymentId()==3L));}

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"ABA","KHQR"})
    void qrMethodsUseExistingDynamicKhqrAndDoNotDuplicateAttempts(String method){
      ownedOrder();
      when(orders.get(1)).thenReturn(new com.example.eshop.common.automation.PaymentOrderSnapshot(1,2,BigDecimal.TEN,"USD","PENDING",method));
      when(repository.findByOrder(1L)).thenReturn(List.of());
      org.springframework.test.util.ReflectionTestUtils.setField(service,"recipient","merchant@bank");
      org.springframework.test.util.ReflectionTestUtils.setField(service,"khqrType",com.example.eshop.payment.bakong.KhqrType.INDIVIDUAL);
      var qrData=new kh.gov.nbc.bakong_khqr.model.KHQRData();qrData.setQr("dynamic-order-qr");qrData.setMd5("b".repeat(32));
      var generated=new kh.gov.nbc.bakong_khqr.model.KHQRResponse<kh.gov.nbc.bakong_khqr.model.KHQRData>();generated.setData(qrData);
      when(bakong.generateQR(any())).thenReturn(generated);
      var created=new com.example.eshop.payment.dto.response.PaymentTransactionResponse();created.setId(3L);
      when(transactionService.createTransaction(any())).thenReturn(created);
      var event=com.example.eshop.common.automation.AutomationEvents.event(com.example.eshop.common.automation.CommerceEvent.Type.ORDER_CREATED,1L,2L,BigDecimal.TEN,method,null);
      service.handle(event);
      assertThat(record.getQrPayload()).isEqualTo("dynamic-order-qr");assertThat(record.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
      var request=org.mockito.ArgumentCaptor.forClass(com.example.eshop.payment.bakong.dto.BakongRequest.class);
      verify(bakong).generateQR(request.capture());assertThat(request.getValue().amount()).isEqualTo(10.0);assertThat(request.getValue().currency()).isEqualTo("USD");
      when(repository.findByOrder(1L)).thenReturn(List.of(record));service.handle(event);
      verify(transactionService,times(1)).createTransaction(any());verify(bakong,times(1)).generateQR(any());
    }
    @Test void cancellationSnapshotRejectsAmbiguousAttempts(){
      when(repository.findByOrder(1L)).thenReturn(List.of(record,record));
      assertThatThrownBy(()->service.snapshotForOrder(1)).hasMessageContaining("reconciliation");
      when(repository.findByOrder(1L)).thenReturn(List.of(record));
      assertThat(service.snapshotForOrder(1).orderId()).isEqualTo(1L);
    }

}
