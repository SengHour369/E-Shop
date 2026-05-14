package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.Model.Payment;
import com.example.learning_spring_security.Repository.OrderRepository;
import com.example.learning_spring_security.Repository.PaymentRepository;
import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private OrderDetail order;
    private Payment payment;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        order = OrderDetail.builder()
                .id(1L)
                .status("PENDING")
                .totalAmount(BigDecimal.valueOf(100))
                .build();
        payment = Payment.builder()
                .id(1L)
                .orderDetail(order)
                .paymentMethod("CREDIT_CARD")
                .amount(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .transactionId("TXN-123456789-123")
                .build();
        paymentRequest = PaymentRequest.builder()
                .paymentMethod("CREDIT_CARD")
                .amount(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void processPayment_ShouldProcessSuccessfully() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(OrderDetail.class))).thenReturn(order);

        // When
        PaymentResponse response = paymentService.processPayment(1L, paymentRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(order);
    }

    @Test
    void processPayment_ShouldThrowException_WhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(1L, paymentRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 1");
    }

    @Test
    void processPayment_ShouldThrowException_WhenOrderAlreadyHasPayment() {
        // Given
        order.setPayment(payment);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(1L, paymentRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order already has a payment");
    }

    @Test
    void getPaymentByOrderId_ShouldReturnPayment_WhenExists() {
        // Given
        when(paymentRepository.findByOrderDetailId(1L)).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPaymentByOrderId(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TXN-123456789-123");
        verify(paymentRepository).findByOrderDetailId(1L);
    }

    @Test
    void getPaymentByOrderId_ShouldThrowException_WhenNotFound() {
        // Given
        when(paymentRepository.findByOrderDetailId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found for order id: 1");
    }

    @Test
    void getPaymentByTransactionId_ShouldReturnPayment_WhenExists() {
        // Given
        when(paymentRepository.findByTransactionId("TXN-123456789-123")).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPaymentByTransactionId("TXN-123456789-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("TXN-123456789-123");
        verify(paymentRepository).findByTransactionId("TXN-123456789-123");
    }

    @Test
    void getPaymentByTransactionId_ShouldThrowException_WhenNotFound() {
        // Given
        when(paymentRepository.findByTransactionId("TXN-123456789-123")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByTransactionId("TXN-123456789-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found with transaction id: TXN-123456789-123");
    }

    @Test
    void updatePaymentStatus_ShouldUpdateSuccessfully() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentResponse response = paymentService.updatePaymentStatus(1L, "FAILED", "TXN-987654321-456");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getTransactionId()).isEqualTo("TXN-987654321-456");
        verify(paymentRepository).save(payment);
    }

    @Test
    void updatePaymentStatus_ShouldThrowException_WhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, "FAILED", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found with id: 1");
    }
}
