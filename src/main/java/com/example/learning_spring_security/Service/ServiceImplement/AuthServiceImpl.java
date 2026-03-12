package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserRepository;

import com.example.learning_spring_security.Service.ServiceStructure.AuthService;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.Register;
import com.example.learning_spring_security.dto.Response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseErrorTemplate create(Register userRequest) {
        // TODO: 3/4/23 validation request data before processing save user
        this.userRequestValidation(userRequest);
         List<String> role = List.of("USER");
        List<Role> roles = roleRepository.findAllByNameIn(role);
        User user = User.builder()
                .username(userRequest.username())
                .password(passwordEncoder.encode(userRequest.password()))
                .fullName(userRequest.fullName())
                .email(userRequest.email())
                .roles(roles)
                .attempt(0)
                .status(Constant.ACT)
                .created(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return this.userMapper(user);
    }

    @Override
    public Optional<Long> findById(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::getId);
    }

    @Override
    public ResponseErrorTemplate logIn(String username) {
        Optional<User> user = userRepository.findFirstByUsernameAndStatus(username, Constant.ACT);

        var msg = String.format(Constant.USER_NAME_NOT_FOUND, user);
        return user.map(this::userMapper)
                .orElse(new ResponseErrorTemplate(msg, Constant.USER_NOT_FOUND_CODE, new Object()));
    }

    @Override
    public void requestPasswordReset(String email) {

    }

    @Override
    public void resetPassword(String token, String newPassword) {

    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {

    }

    public ResponseErrorTemplate userMapper(User user) {
        RegisterResponse userResponse = new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Role::getName).toList(),
                user.getCreated()
        );
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, userResponse);
    }
    private void userRequestValidation(Register userRequest) {

        // TODO: 3/4/23 password must be not null or blank
        if(ObjectUtils.isEmpty(userRequest.password())) {
            throw new CustomMessageException("Password can't be blank or null",
                    String.valueOf(HttpStatus.BAD_REQUEST));
        }

        // TODO: 3/4/23 username and email must be not duplicate
        Optional<User> user = userRepository.findFirstByUsernameOrEmail(userRequest.username(),
                userRequest.email());
        if(user.isPresent()){
            throw new CustomMessageException("Username or Email already exists.",
                    String.valueOf(HttpStatus.BAD_REQUEST));
        }



    }
}

