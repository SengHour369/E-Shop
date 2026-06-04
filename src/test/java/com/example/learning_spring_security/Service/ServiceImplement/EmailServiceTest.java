package com.example.learning_spring_security.Service.ServiceImplement;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "test@example.com");
    }

    @Test
    void sendSimpleEmail_ShouldSendSuccessfully() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // When
        emailNotificationService.sendSimpleEmail(to, subject, body);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_ShouldThrowRuntimeException_WhenMailSendingFails() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String body = "Test Body";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailNotificationService.sendSimpleEmail(to, subject, body))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email");
    }

    @Test
    void sendHtmlEmail_ShouldSendSuccessfully() throws MessagingException {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String htmlBody = "<html><body>Test HTML</body></html>";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailNotificationService.sendHtmlEmail(to, subject, htmlBody);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlEmail_ShouldThrowRuntimeException_WhenMessagingExceptionOccurs() throws MessagingException {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String htmlBody = "<html><body>Test HTML</body></html>";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        MimeMessageHelper helper = mock(MimeMessageHelper.class);
        doThrow(new MessagingException("MIME error")).when(mailSender).send(mimeMessage);

        // When & Then
        assertThatThrownBy(() -> emailNotificationService.sendHtmlEmail(to, subject, htmlBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send HTML email");
    }

    @Test
    void sendEmailToMultiple_ShouldSendSuccessfully() {
        // Given
        String[] recipients = {"recipient1@example.com", "recipient2@example.com"};
        String subject = "Test Subject";
        String body = "Test Body";

        // When
        emailNotificationService.sendEmailToMultiple(recipients, subject, body);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailToMultiple_ShouldThrowRuntimeException_WhenMailSendingFails() {
        // Given
        String[] recipients = {"recipient1@example.com", "recipient2@example.com"};
        String subject = "Test Subject";
        String body = "Test Body";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailNotificationService.sendEmailToMultiple(recipients, subject, body))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email to multiple recipients");
    }

    @Test
    void sendRegistrationConfirmationEmail_ShouldSendEmail() {
        // Given
        String to = "user@example.com";
        String username = "testuser";

        // When
        emailNotificationService.sendRegistrationConfirmationEmail(to, username);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmationEmail_ShouldSendHtmlEmail() throws MessagingException {
        // Given
        String to = "user@example.com";
        String orderNumber = "ORD-123";
        Double totalAmount = 99.99;
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailNotificationService.sendOrderConfirmationEmail(to, orderNumber, totalAmount);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_ShouldSendHtmlEmail() throws MessagingException {
        // Given
        String to = "user@example.com";
        String resetLink = "http://example.com/reset";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailNotificationService.sendPasswordResetEmail(to, resetLink);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderShippedEmail_ShouldSendHtmlEmail() throws MessagingException {
        // Given
        String to = "user@example.com";
        String orderNumber = "ORD-123";
        String trackingNumber = "TRACK-456";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailNotificationService.sendOrderShippedEmail(to, orderNumber, trackingNumber);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrderCancellationEmail_ShouldSendHtmlEmail() throws MessagingException {
        // Given
        String to = "user@example.com";
        String orderNumber = "ORD-123";
        String reason = "Out of stock";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailNotificationService.sendOrderCancellationEmail(to, orderNumber, reason);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}
