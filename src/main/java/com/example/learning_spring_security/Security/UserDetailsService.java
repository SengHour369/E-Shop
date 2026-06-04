package com.example.learning_spring_security.Security;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info(" loadUserByUsername called with: {}", username);
        return this.customUserDetail(username);
    }

    private UserDetailsImpl customUserDetail(String usernameOrEmail) {
        log.info(" Searching for user by username or email: {}", usernameOrEmail);

        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);

        if (user.isEmpty()) {
            log.warn(" Username or Email {} not found or inactive", usernameOrEmail);
            throw new CustomMessageException("Unauthorized", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }

        User foundUser = user.get();
        log.info(" User found: {} (ID: {}, Email: {})",
                foundUser.getUsername(), foundUser.getId(), foundUser.getEmail());

        return new UserDetailsImpl(
                foundUser.getUsername(),
                foundUser.getEmail(),
                foundUser.getPassword(),
                foundUser.getRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }


    public void saveUserAttemptAuthentication(String usernameOrEmail) {
        log.info(" Recording authentication attempt for: {}", usernameOrEmail);

        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);

        if (user.isPresent()) {
            User foundUser = user.get();
            int attempt = foundUser.getAttempt() + 1;
            foundUser.setAttempt(attempt);

            if (foundUser.getAttempt() > 3) {
                log.warn(" User {} exceeded 3 attempts. Blocking account...", usernameOrEmail);
                foundUser.setStatus(Constant.BLK);
            }

            userRepository.save(foundUser);
            log.info(" Attempt count updated to {} for user: {}", attempt, foundUser.getUsername());
        } else {
            log.warn("User not found for attempt recording: {}", usernameOrEmail);
        }
    }


    public void updateAttempt(String usernameOrEmail) {
        log.info(" Resetting attempt count for: {}", usernameOrEmail);

        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);

        if (user.isPresent()) {
            User foundUser = user.get();
            foundUser.setAttempt(0);
            userRepository.save(foundUser);
            log.info(" Attempt count reset to 0 for user: {}", foundUser.getUsername());
        } else {
            log.warn(" User not found for attempt reset: {}", usernameOrEmail);
        }
    }
}