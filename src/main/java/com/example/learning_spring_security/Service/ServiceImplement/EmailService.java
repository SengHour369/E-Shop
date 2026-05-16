package com.example.learning_spring_security.Service.ServiceImplement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Send email to multiple recipients
     */
    public void sendEmailToMultiple(String[] recipients, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {} recipients", recipients.length);
        } catch (Exception e) {
            log.error("Failed to send email to multiple recipients", e);
            throw new RuntimeException("Failed to send email to multiple recipients", e);
        }
    }

    /**
     * Send registration confirmation email
     */
    public void sendRegistrationConfirmationEmail(String to, String username) {
        String subject = "Welcome to E_Shop - Account Created";
        String body = String.format(
                "Dear %s,\n\n" +
                "Welcome to E_Shop! Your account has been successfully created.\n\n" +
                "Username: %s\n\n" +
                "You can now login to your account and start shopping.\n\n" +
                "Best regards,\n" +
                "E_Shop Team",
                username, username
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send order confirmation email
     */
    public void sendOrderConfirmationEmail(String to, String orderNumber, Double totalAmount) {
        String subject = "Order Confirmation - E_Shop";
        String htmlBody = String.format(
                "<html>" +
                "<body>" +
                "<h2>Order Confirmation</h2>" +
                "<p>Dear Customer,</p>" +
                "<p>Your order has been successfully placed.</p>" +
                "<p><strong>Order Number:</strong> %s</p>" +
                "<p><strong>Total Amount:</strong> $%.2f</p>" +
                "<p>You will receive updates about your order status via email.</p>" +
                "<p>Thank you for shopping with E_Shop!</p>" +
                "<p>Best regards,<br/>E_Shop Team</p>" +
                "</body>" +
                "</html>",
                orderNumber, totalAmount
        );
        sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Password Reset - E_Shop";
        String htmlBody = String.format(
                "<html>" +
                "<body>" +
                "<h2>Password Reset Request</h2>" +
                "<p>Dear User,</p>" +
                "<p>We received a request to reset your password. Click the link below to reset it:</p>" +
                "<p><a href=\"%s\">Reset Your Password</a></p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "<p>Best regards,<br/>E_Shop Team</p>" +
                "</body>" +
                "</html>",
                resetLink
        );
        sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Send order shipped notification
     */
    public void sendOrderShippedEmail(String to, String orderNumber, String trackingNumber) {
        String subject = "Your Order Has Been Shipped - E_Shop";
        String htmlBody = String.format(
                "<html>" +
                "<body>" +
                "<h2>Order Shipped</h2>" +
                "<p>Dear Customer,</p>" +
                "<p>Your order has been shipped!</p>" +
                "<p><strong>Order Number:</strong> %s</p>" +
                "<p><strong>Tracking Number:</strong> %s</p>" +
                "<p>You can track your package using the tracking number above.</p>" +
                "<p>Thank you for your purchase!</p>" +
                "<p>Best regards,<br/>E_Shop Team</p>" +
                "</body>" +
                "</html>",
                orderNumber, trackingNumber
        );
        sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Send order cancellation email
     */
    public void sendOrderCancellationEmail(String to, String orderNumber, String reason) {
        String subject = "Order Cancelled - E_Shop";
        String htmlBody = String.format(
                "<html>" +
                "<body>" +
                "<h2>Order Cancelled</h2>" +
                "<p>Dear Customer,</p>" +
                "<p>Your order has been cancelled.</p>" +
                "<p><strong>Order Number:</strong> %s</p>" +
                "<p><strong>Reason:</strong> %s</p>" +
                "<p>If you have any questions, please contact our support team.</p>" +
                "<p>Best regards,<br/>E_Shop Team</p>" +
                "</body>" +
                "</html>",
                orderNumber, reason
        );
        sendHtmlEmail(to, subject, htmlBody);
    }
}

