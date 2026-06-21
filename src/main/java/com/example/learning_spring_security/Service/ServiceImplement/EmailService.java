package com.example.learning_spring_security.Service.ServiceImplement;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {


    private final JavaMailSender emailSender;

    @Value("${app.base-url:http://e-shop-1-m034.onrender.com}")
    private String baseUrl;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String subject, String htmlMessage) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlMessage, true);

        emailSender.send(message);
        log.info("Email sent to: {}", to);
    }

    /**
     * Send verification email with token (for link-based verification)
     */
    @Async
    public void sendVerificationEmailWithToken(String toEmail, String token) {
        try {
            String verificationLink = baseUrl + "/api/v1/public/verify?email=" + toEmail + "&code=" + token;

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your email address");
            helper.setText(buildEmailBody(verificationLink), true);

            emailSender.send(message);
            log.info(" Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error(" Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send verification code email (for code-based verification)
     */
    @Async
    public void sendVerificationCodeEmail(String toEmail, String code) {
        try {
            String subject = "Account Verification";
            String htmlMessage = buildVerificationCodeEmailBody(code);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMessage, true);

            emailSender.send(message);
            log.info(" Verification code email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error(" Failed to send verification code email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = baseUrl + "/api/v1/public/reset-password?token=" + token;

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your password");
            helper.setText(buildPasswordResetBody(resetLink), true);

            emailSender.send(message);
            log.info(" Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error(" Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String verificationLink) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>Email Verification</h2>
                    <p>Thank you for registering. Please click the button below to verify your email address.</p>
                    <p>This link will expire in <strong>24 hours</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background-color:#4CAF50;
                              color:white; text-decoration:none; border-radius:4px;">
                      Verify Email
                    </a>
                    <p>Or copy this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>If you did not create an account, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(verificationLink, verificationLink, verificationLink);
    }

    private String buildVerificationCodeEmailBody(String code) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #333;">Welcome to our app!</h2>
                    <p style="font-size: 16px;">Please enter the verification code below to continue:</p>
                    <div style="background-color: #f5f5f5; padding: 20px; border-radius: 5px; text-align: center;">
                        <h3 style="color: #333;">Verification Code:</h3>
                        <p style="font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px;">%s</p>
                        <p style="font-size: 14px; color: #666;">This code will expire in <strong>15 minutes</strong>.</p>
                    </div>
                    <p style="margin-top: 20px;">If you did not create an account, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(code);
    }

    private String buildPasswordResetBody(String resetLink) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>Password Reset</h2>
                    <p>We received a request to reset your password. Click the button below to proceed.</p>
                    <p>This link will expire in <strong>1 hour</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background-color:#2196F3;
                              color:white; text-decoration:none; border-radius:4px;">
                      Reset Password
                    </a>
                    <p>Or copy this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>If you did not request a password reset, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(resetLink, resetLink, resetLink);
    }
}