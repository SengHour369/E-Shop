package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Model.RefreshToken;
import com.example.learning_spring_security.Model.PasswordResetToken;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.PasswordResetTokenRepository;
import com.example.learning_spring_security.Repository.RefreshTokenRepository;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Security.UserDetailsImpl;
import com.example.learning_spring_security.Service.ServiceStructure.AuthService;
import com.example.learning_spring_security.dto.Request.Login;
import com.example.learning_spring_security.dto.Request.RefreshTokenRequest;
import com.example.learning_spring_security.dto.Request.ForgotPasswordRequest;
import com.example.learning_spring_security.dto.Request.ResetPasswordRequest;
import com.example.learning_spring_security.dto.Response.AuthenticationResponse;
import com.example.learning_spring_security.dto.Request.Register;
import com.example.learning_spring_security.dto.Response.RegisterResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.VerifyUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public ResponseErrorTemplate create(Register userRequest) {
        log.info("Starting registration for user: {}", userRequest.username());
        this.userRequestValidation(userRequest);
        List<String> role = List.of("USER");
        List<Role> roles = roleRepository.findAllByNameIn(role);

        User user = User.builder()
                .username(userRequest.username())
                .password(passwordEncoder.encode(userRequest.password()))
                .fullName(userRequest.fullName())
                .email(userRequest.email())
                .roles(roles)
                .phone(userRequest.phone())
                .attempt(0)
                .status(Constant.ACT)
                .deleted(false)
                .enabled(false)
                .build();

        user.setCreatedAt(LocalDateTime.now());
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // Email is sent after save so a mail failure does not roll back the registration
        sendVerificationEmail(savedUser);

        RegisterResponse registerResponse = userMapper(savedUser);

        return ResponseErrorTemplate.builder()
                .message("User registered successfully. Please check your email to verify your account.")
                .code(Constant.SUC_CODE)
                .object(registerResponse)
                .build();
    }

    /**
     * @param username
     * @return
     */
    @Override
    public Optional<Long> findById(String username) {
        return Optional.empty();
    }

    @Transactional
    public AuthenticationResponse verifyUser(VerifyUserDto input) {
        log.info("verifyUser() called for email: {}", input.getEmail());
        log.info("Verification code: {}", input.getVerificationCode());

        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isEmpty()) {
            log.error("User not found: {}", input.getEmail());
            throw new CustomMessageException("User not found",
                    String.valueOf(HttpStatus.NOT_FOUND.value()));
        }

        User user = optionalUser.get();
        log.info("User found: {}", user.getUsername());

        if (user.isEnabled()) {
            log.warn("Account already verified: {}", user.getEmail());
            throw new CustomMessageException("Account is already verified",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Verification code expired for: {}", user.getEmail());
            throw new CustomMessageException("Verification code has expired. Please request a new one.",
                    String.valueOf(HttpStatus.GONE.value()));
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            log.warn("Invalid verification code for: {}", user.getEmail());
            throw new CustomMessageException("Invalid verification code",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified successfully for: {}", user.getEmail());

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        String accessToken = jwtService.generateToken(userDetails);

        refreshTokenRepository.deleteByUser(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        return AuthenticationResponse.builder()
                .id(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRoles().stream().map(Role::getName).findFirst().orElse("USER"))
                .build();
    }

    @Transactional
    public AuthenticationResponse resendVerificationCode(String email) {
        log.info("Resending verification code to: {}", email);

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new CustomMessageException("User not found",
                    String.valueOf(HttpStatus.NOT_FOUND.value()));
        }

        User user = optionalUser.get();

        if (user.isEnabled()) {
            throw new CustomMessageException("Account is already verified",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        sendVerificationEmail(user);

        log.info("Verification code resent to: {}", email);

        return AuthenticationResponse.builder()
                .id(user.getId())
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRoles().stream().map(Role::getName).findFirst().orElse("USER"))
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(Login input) {

        String value = input.CriteriaValue();
        String password = input.Password();

        log.info("Authenticating user: {}", value);
        log.info("Password user: {}", password);

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(value, password)
                );

        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findByUsernameOrEmailAndStatus(value, Constant.ACT)
                .orElseThrow(() ->
                        new CustomMessageException(
                                "User not found",
                                String.valueOf(HttpStatus.UNAUTHORIZED.value())
                        )
                );

        if (!user.isEnabled()) {
            throw new CustomMessageException("Please verify email",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        if (Constant.BLK.equals(user.getStatus())) {
            throw new CustomMessageException("Account locked",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        user.setAttempt(0);
        userRepository.save(user);

        String accessToken = jwtService.generateToken(userDetails);

        refreshTokenRepository.deleteByUser(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        return AuthenticationResponse.builder()
                .id(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(
                        user.getRoles().stream()
                                .map(Role::getName)
                                .findFirst()
                                .orElse("USER")
                )
                .build();
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token...");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomMessageException("Invalid refresh token",
                        String.valueOf(HttpStatus.UNAUTHORIZED.value())));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomMessageException("Refresh token has expired. Please log in again.",
                    String.valueOf(HttpStatus.GONE.value()));
        }

        User user = refreshToken.getUser();

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        String newAccessToken = jwtService.generateToken(userDetails);

        refreshTokenRepository.delete(refreshToken);
        String newRefreshToken = generateAndSaveRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("USER");

        return AuthenticationResponse.builder()
                .id(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(role)
                .build();
    }

    @Transactional
    public AuthenticationResponse logout(RefreshTokenRequest request) {
        log.info("Logging out...");

        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);

        log.info("Logout successful");

        return AuthenticationResponse.builder()
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public AuthenticationResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            // Save committed before sending email — mail failure won't roll back the token
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("Password reset email sent to: {}", request.getEmail());
        }

        return AuthenticationResponse.builder()
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public AuthenticationResponse resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password with token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new CustomMessageException("Invalid or expired reset token",
                        String.valueOf(HttpStatus.BAD_REQUEST.value())));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new CustomMessageException("Reset token has expired. Please request a new one.",
                    String.valueOf(HttpStatus.GONE.value()));
        }

        if (resetToken.isUsed()) {
            throw new CustomMessageException("Reset token has already been used.",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());

        return AuthenticationResponse.builder()
                .id(user.getId())
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRoles().stream().map(Role::getName).findFirst().orElse("USER"))
                .build();
    }

    private String generateAndSaveRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private void sendVerificationEmail(User user) {
        emailService.sendVerificationCodeEmail(user.getEmail(), user.getVerificationCode());
        log.info("Verification email sent to: {}", user.getEmail());
    }

    private void userRequestValidation(Register userRequest) {
        if (ObjectUtils.isEmpty(userRequest.password())) {
            throw new CustomMessageException("Password can't be blank or null",
                    String.valueOf(HttpStatus.BAD_REQUEST));
        }

        Optional<User> user = userRepository.findFirstByUsernameOrEmail(userRequest.username(),
                userRequest.email());
        if (user.isPresent()) {
            throw new CustomMessageException("Username or Email already exists.",
                    String.valueOf(HttpStatus.BAD_REQUEST));
        }
    }

    public RegisterResponse userMapper(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .created(user.getCreatedAt())
                .build();
    }
}