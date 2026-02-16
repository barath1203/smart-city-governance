package com.smartcity.governance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartcity.governance.security.JwtAuthFilter;
import com.smartcity.governance.security.JwtService;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtService jwtService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // 🔴 Disable CSRF (for REST APIs)
            .csrf(csrf -> csrf.disable())

            // 🔴 Make app stateless (VERY IMPORTANT for JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 🔐 Authorization Rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/complaints/create/**").hasRole("CITIZEN")
                .requestMatchers("/api/complaints/user/**").hasRole("CITIZEN")
                .requestMatchers("/api/complaints/update-status/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/complaints/all").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // ❌ Disable default login page
            .formLogin(form -> form.disable())

            // ❌ Disable HTTP Basic
            .httpBasic(basic -> basic.disable());

        // ✅ Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
        http.addFilterBefore(
                new JwtAuthFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
