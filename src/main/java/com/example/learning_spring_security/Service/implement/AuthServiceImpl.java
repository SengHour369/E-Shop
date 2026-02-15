package com.example.learning_spring_security.Service.implement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.AuthService;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.UserResponse;
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
    public ResponseErrorTemplate create(UserRequest userRequest) {
        // TODO: 3/4/23 validation request data before processing save user
        this.userRequestValidation(userRequest);

        List<Role> roles = roleRepository.findAllByNameIn(userRequest.roles());
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
    public ResponseErrorTemplate findById(Long id) {
        Optional<User> user = userRepository.findById(id);
        var msg = String.format(Constant.USER_ID_NOT_FOUND, id);
        return user.map(this::userMapper)
                .orElse(new ResponseErrorTemplate(msg, Constant.USER_NOT_FOUND_CODE, new Object()));
    }

    @Override
    public ResponseErrorTemplate logIn(String username) {
        Optional<User> user = userRepository.findFirstByUsernameAndStatus(username, Constant.ACT);

        var msg = String.format(Constant.USER_NAME_NOT_FOUND, user);
        return user.map(this::userMapper)
                .orElse(new ResponseErrorTemplate(msg, Constant.USER_NOT_FOUND_CODE, new Object()));
    }

    public ResponseErrorTemplate userMapper(User user) {
        UserResponse userResponse = new UserResponse(
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Role::getName).toList(),
                user.getCreated()
        );
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, userResponse);
    }
    private void userRequestValidation(UserRequest userRequest) {

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

        // TODO: 3/4/23 check role valid request
        List<String> roles = roleRepository.findAll().stream().map(Role::getName).toList();
        for(var role : userRequest.roles()){
            if(!roles.contains(role)) {
                throw new CustomMessageException("Role is invalid request.",
                        String.valueOf(HttpStatus.BAD_REQUEST));
            }
        }
    }
}

