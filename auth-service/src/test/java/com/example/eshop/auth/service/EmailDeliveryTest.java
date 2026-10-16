package com.example.eshop.auth.service;
import com.example.eshop.auth.service.impl.EmailService;
import com.example.eshop.auth.dto.request.Register;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Properties;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class EmailDeliveryTest {
    private EmailService service(JavaMailSender sender) {
        var service = new EmailService(sender);
        ReflectionTestUtils.setField(service,"fromEmail","sender@example.test");
        ReflectionTestUtils.setField(service,"baseUrl","http://localhost:8080");
        return service;
    }
    @Test void verificationSendFailurePropagatesToRegistration() {
        var sender=mock(JavaMailSender.class);var message=new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(message);
        assertThatThrownBy(()->service(sender).sendVerificationCodeEmail("recipient@example.test","123456")).isInstanceOf(MailSendException.class);
    }
    @Test void invalidRecipientDoesNotReportSuccess() {
        var sender=mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        assertThatThrownBy(()->service(sender).sendVerificationCodeEmail("invalid@@address", "123456")).isInstanceOf(MailSendException.class);
        verify(sender,never()).send(any(MimeMessage.class));
    }
    @Test void resetEmailUsesTheBrowserResetPage() throws Exception {
        var sender=mock(JavaMailSender.class);var message=new MimeMessage(Session.getInstance(new Properties()));when(sender.createMimeMessage()).thenReturn(message);
        service(sender).sendPasswordResetEmail("recipient@example.test","reset-token");message.saveChanges();
        assertThat(body(message)).contains("reset-password-page?token=reset-token");
        verify(sender).send(message);
    }
    private String body(jakarta.mail.Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) return text;
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int index=0; index<multipart.getCount(); index++) result.append(body(multipart.getBodyPart(index)));
            return result.toString();
        }
        return "";
    }
    @Test void registrationNormalizesEmailAndUsername() {
        var request=new Register(" alice ","password"," Alice@Example.test "," 123 "," Alice ");
        assertThat(request.email()).isEqualTo("alice@example.test");assertThat(request.username()).isEqualTo("alice");
    }
}
