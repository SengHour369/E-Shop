package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.Security.UserDetailsImpl;
import com.example.learning_spring_security.Service.ServiceStructure.AuthService;
import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.AuthenticationResponse;
import com.example.learning_spring_security.dto.Response.RegisterResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
import java.util.*;
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

    // Repositories for groups and permissions
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    // Constants
    private static final String DEFAULT_ROLE_NAME = "USER";
    private static final String DEFAULT_GROUP_CODE = "USR";
    private static final List<String> DEFAULT_PERMISSION_CODES = Arrays.asList(
            "CART_ADD_ITEM",
            "ORDER_CREATE",
            "BAKONG_QR",
            "ADDRESS_CREATE",
            "ADDRESS_VIEW",
            "ADDRESS_UPDATE",
            "ADDRESS_DELETE"
    );
    // --- Register ---
    @Override
    @Transactional
    public ResponseErrorTemplate create(Register userRequest) {
        log.info("Starting registration for user: {}", userRequest.username());
        this.userRequestValidation(userRequest);

        // 1. Ensure default role exists
        Role defaultRole = getOrCreateDefaultRole();

        // 2. Build user entity
        User user = User.builder()
                .username(userRequest.username())
                .password(passwordEncoder.encode(userRequest.password()))
                .fullName(userRequest.fullName())
                .email(userRequest.email())
                .roles(Collections.singletonList(defaultRole))
                .phone(userRequest.phone())
                .attempt(0)
                .status(Constant.ACT)
                .deleted(false)
                .enabled(false)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        // 3. Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // 4. Assign to default group (create if missing)
        Group defaultGroup = getOrCreateDefaultGroup();
        UserGroup userGroup = UserGroup.builder()
                .userId(savedUser.getId())
                .groupId(defaultGroup.getId())
                .isActive(true)
                .isDelete(false)
                .build();
        userGroupRepository.save(userGroup);
        log.info("Assigned user {} to group {}", savedUser.getUsername(), DEFAULT_GROUP_CODE);

        // 5. Assign default permissions (fetch each by code)
        assignDefaultPermissions(savedUser);

        // 6. Send verification email
        sendVerificationEmail(savedUser);

        // 7. Build response
        RegisterResponse registerResponse = userMapper(savedUser);
        return ResponseErrorTemplate.builder()
                .message("User registered successfully. Please check your email to verify your account.")
                .code(Constant.SUC_CODE)
                .object(registerResponse)
                .build();
    }

    // --- Verify user ---
    @Transactional
    public AuthenticationResponse verifyUser(VerifyUserDto input) {
        log.info("verifyUser() called for email: {}", input.getEmail());

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new CustomMessageException("User not found",
                        String.valueOf(HttpStatus.NOT_FOUND.value())));

        if (user.isEnabled()) {
            throw new CustomMessageException("Account is already verified",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomMessageException("Verification code has expired. Please request a new one.",
                    String.valueOf(HttpStatus.GONE.value()));
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new CustomMessageException("Invalid verification code",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified successfully for: {}", user.getEmail());

        // Generate tokens
        UserDetailsImpl userDetails = buildUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        refreshTokenRepository.deleteByUser(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // --- Resend verification code ---
    @Transactional
    public AuthenticationResponse resendVerificationCode(String email) {
        log.info("Resending verification code to: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomMessageException("User not found",
                        String.valueOf(HttpStatus.NOT_FOUND.value())));

        if (user.isEnabled()) {
            throw new CustomMessageException("Account is already verified",
                    String.valueOf(HttpStatus.BAD_REQUEST.value()));
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        sendVerificationEmail(user);

        return AuthenticationResponse.builder()
                .id(user.getId())
                .tokenType("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRoles().stream().map(Role::getName).findFirst().orElse("USER"))
                .build();
    }

    // --- Authenticate (Login) ---
    @Transactional
    public AuthenticationResponse authenticate(Login input) {
        String value = input.CriteriaValue();
        String password = input.Password();

        log.info("Authenticating user: {}", value);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(value, password)
            );
        } catch (Exception e) {
            // Increment attempt count on failure (optional)
            userRepository.findByUsernameOrEmailAndStatus(value, Constant.ACT)
                    .ifPresent(user -> {
                        user.setAttempt(user.getAttempt() + 1);
                        userRepository.save(user);
                        // Optionally lock account after max attempts
                    });
            throw new CustomMessageException("Invalid username or password",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByUsernameOrEmailAndStatus(value, Constant.ACT)
                .orElseThrow(() -> new CustomMessageException("User not found",
                        String.valueOf(HttpStatus.UNAUTHORIZED.value())));

        if (!user.isEnabled()) {
            throw new CustomMessageException("Please verify your email before logging in.",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        if (Constant.BLK.equals(user.getStatus())) {
            throw new CustomMessageException("Account locked",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        // Reset attempts on success
        user.setAttempt(0);
        userRepository.save(user);

        String accessToken = jwtService.generateToken(userDetails);
        refreshTokenRepository.deleteByUser(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // --- Refresh token ---
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
        UserDetailsImpl userDetails = buildUserDetails(user);

        String newAccessToken = jwtService.generateToken(userDetails);
        refreshTokenRepository.delete(refreshToken);
        String newRefreshToken = generateAndSaveRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());
        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // --- Logout ---
    @Transactional
    public AuthenticationResponse logout(RefreshTokenRequest request) {
        log.info("Logging out...");
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
        return AuthenticationResponse.builder().tokenType("Bearer").build();
    }

    // --- Forgot password ---
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

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
                log.info("Password reset email sent to: {}", request.getEmail());
            } catch (Exception e) {
                // If email fails, delete the token to allow retry
                passwordResetTokenRepository.delete(resetToken);
                log.error("Failed to send password reset email", e);
                throw new CustomMessageException("Failed to send reset email. Please try again later.",
                        String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            }
        }

        // Always return a generic message for security (don't reveal if email exists)
        return AuthenticationResponse.builder()
                .tokenType("Bearer")
                .build();
    }

    // --- Reset password ---
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

    // --- Private helper methods ---

    private Role getOrCreateDefaultRole() {
        return roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseGet(() -> {
                    Role role = Role.builder().name(DEFAULT_ROLE_NAME).build();
                    return roleRepository.save(role);
                });
    }

    private Group getOrCreateDefaultGroup() {
        return groupRepository.findByGroupCode(DEFAULT_GROUP_CODE)
                .orElseGet(() -> {
                    Group group = Group.builder()
                            .groupCode(DEFAULT_GROUP_CODE)
                            .name("User Group")
                            .description("Default group for new users")
                            .status(Constant.ACT)
                            .isActive(true)
                            .isDelete(false)
                            .build();
                    group.setCreatedAt(LocalDateTime.now());
                    return groupRepository.save(group);
                });
    }

    private void assignDefaultPermissions(User user) {
        // Fetch each permission by code and create UserPermission entries
        for (String code : DEFAULT_PERMISSION_CODES) {
            functionPermissionRepository.findByFuncCodeAndIsDeleteFalse(code)
                    .ifPresent(fp -> {
                        UserPermission up = UserPermission.builder()
                                .userId(user.getId())
                                .funcId(fp.getFuncId())
                                .isActive(true)
                                .isDelete(false)
                                .build();
                        userPermissionRepository.save(up);
                        log.info("Assigned permission {} to user {}", code, user.getUsername());
                    });
        }
    }

    private UserDetailsImpl buildUserDetails(User user) {
        return new UserDetailsImpl(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }

    private AuthenticationResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
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

    private RegisterResponse userMapper(User user) {
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

    // --- Unused methods from interface (if any) ---
    @Override
    public Optional<Long> findById(String username) {
        return Optional.empty();
    }
}