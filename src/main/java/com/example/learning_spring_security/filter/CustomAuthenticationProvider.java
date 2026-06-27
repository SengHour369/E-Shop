package com.example.learning_spring_security.filter;


import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationProvider implements AuthenticationProvider {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final UserRepository userRepository;
    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        System.out.println("username: " + username);
        System.out.println("password: " + password);

        User user = userRepository
                .findByUsernameOrEmailAndStatus(username, Constant.ACT)
                .orElseThrow(() ->
                        new CustomMessageException("User not found", "401"));

        if (!passwordEncoder().matches(password, user.getPassword())) {
            throw new CustomMessageException("Invalid password", "401");
        }

        List<GrantedAuthority> authorities =
                grantedAuthorities(user.getRoles().stream().toList());

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                authorities
        );
    }
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private List<GrantedAuthority> grantedAuthorities(List<Role> roles) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        Set<String> permissions = new HashSet<>();

        if(!roles.isEmpty()){
            roles.forEach(role -> {
                permissions.add(role.getName());
            });
        }

        permissions.forEach(permission -> grantedAuthorities.add(new SimpleGrantedAuthority(permission)));
        return grantedAuthorities;
    }

}
