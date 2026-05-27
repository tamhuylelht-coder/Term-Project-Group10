package com.vinuni.roombooking.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.vinuni.roombooking.service.DatabaseConnector;

/**
 * Spring Security configuration for Vaadin.
 *
 * HTTP requests are permitted and authentication is performed explicitly in
 * `LoginView` via `AuthenticationManager.authenticate(...)`.
 * Vaadin view guards in `MainLayout` and `AdminView` enforce access control.
 *
 * This approach is used because the Vaadin/Spring Security integration in
 * this stack does not support the standard filter-based auth flow reliably.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * DB-backed UserDetailsService. Calls {@link DatabaseConnector#findUserAuthByName(String)},
     * which returns a 3-element List of [user_name, user_password, user_role] (or null if missing).
     */
    @Bean
    public UserDetailsService userDetailsService(DatabaseConnector db) {
        return username -> {
            List<String> auth = db.findUserAuthByName(username);
            if (auth == null || auth.size() < 3) {
                throw new UsernameNotFoundException("No user: " + username);
            }
            String storedUsername = auth.get(0);
            String passwordHash   = auth.get(1);
            String role           = auth.get(2); // STUDENT / STAFF / ADMIN
            return User.withUsername(storedUsername)
                    .password(passwordHash)
                    .roles(role)
                    .build();
        };
    }
}
