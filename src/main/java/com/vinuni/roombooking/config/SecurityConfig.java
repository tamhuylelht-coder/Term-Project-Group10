package com.vinuni.roombooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * The HTTP filter chain permits all requests so Vaadin's UIDL channel is never
 * intercepted. Authentication still happens — LoginView calls
 * AuthenticationManager.authenticate() on submit, which checks the typed
 * credentials against the InMemoryUserDetailsManager below using
 * BCryptPasswordEncoder. Wrong password = BadCredentialsException, surfaced in
 * the LoginView as an error notification.
 *
 * Access control happens at the Vaadin view layer instead of the filter layer:
 *   - MainLayout's beforeEnter redirects to LoginView when SessionUtil is empty
 *   - AdminView's beforeEnter forwards non-Admin users away from /admin
 *
 * Why not enforce auth at the filter layer (the textbook Spring Security setup)?
 * VaadinSecurityConfigurer in Vaadin 25.1.5 doesn't integrate cleanly with
 * Spring Security 7.0.5 — programmatic SecurityContext persistence does not
 * stick across Vaadin UIDL requests, and a LoginForm-based flow has its own
 * quirks in this combo. Permit-all + AuthenticationManager-based validation +
 * Vaadin view guards is the configuration that actually works end-to-end in
 * this stack. Future versions of Vaadin / Spring may make
 * VaadinSecurityConfigurer.vaadin().loginView(LoginView.class) viable; until
 * then, this is the durable answer.
 *
 * Replace the InMemoryUserDetailsManager with a DB-backed UserDetailsService
 * once a real UserRepository lands. The rest of this file does not need to
 * change at that point.
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
     * In-memory user store mirroring the three demo accounts in {@code ui.DemoData}.
     * Roles match {@link com.vinuni.roombooking.model.User#getUserType()}.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails alice = User.withUsername("alice")
                .password(encoder.encode("pass"))
                .roles("STUDENT")
                .build();
        UserDetails bob = User.withUsername("bob")
                .password(encoder.encode("pass"))
                .roles("STAFF")
                .build();
        UserDetails carol = User.withUsername("carol")
                .password(encoder.encode("pass"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(alice, bob, carol);
    }
}
