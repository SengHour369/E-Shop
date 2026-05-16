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
    public  UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.customUserDetail(username);
    }

    private UserDetailsImpl customUserDetail(String usernameOrEmail) {
        // Try to find by username or email (case-insensitive)
        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);

        if(user.isEmpty()){
            log.warn("Username or Email {} unauthorized", usernameOrEmail);
            throw new CustomMessageException("Unauthorized", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }


        return new UserDetailsImpl(
                user.get().getUsername(),
                user.get().getPassword(),
                user.get().getRoles()
                        .stream().map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()));
    }

    public void saveUserAttemptAuthentication(String usernameOrEmail) {
        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);
        if(user.isPresent()) {
            int attempt = user.get().getAttempt() + 1;
            user.get().setAttempt(attempt);
            user.get().setUpdatedAt(LocalDateTime.now());
            if(user.get().getAttempt() > 3) {
                log.warn("User {} update status to blocked", usernameOrEmail);
                user.get().setStatus(Constant.BLK);
            }
            userRepository.save(user.get());
        }
    }

    public void updateAttempt(String usernameOrEmail) {
        Optional<User> user = userRepository.findByUsernameOrEmailAndStatus(usernameOrEmail, Constant.ACT);
        if(user.isPresent()) {
            user.get().setAttempt(0);
            user.get().setUpdatedAt(LocalDateTime.now());
            userRepository.save(user.get());
        }
    }

}
