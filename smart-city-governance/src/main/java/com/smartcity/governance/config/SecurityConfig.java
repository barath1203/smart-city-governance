package com.smartcity.governance.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.smartcity.governance.security.JwtAuthFilter;
import com.smartcity.governance.security.JwtService;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtService jwtService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/dh/**").hasRole("DEPARTMENT_HEAD")
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/notifications/**").permitAll()
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/officer/**").hasRole("OFFICER")
                .requestMatchers("/api/complaints/create/**").hasRole("CITIZEN")
                .requestMatchers("/api/complaints/user/**").hasRole("CITIZEN")
                .requestMatchers("/api/complaints/update-status/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/complaints/all").hasAnyRole("OFFICER", "ADMIN", "DEPARTMENT_HEAD")
                .requestMatchers("/api/complaints/request-coordination/**").hasRole("OFFICER")
                .requestMatchers("/api/chatbot/faq").permitAll()
                .requestMatchers("/api/chatbot/status/**").hasRole("CITIZEN")
                .requestMatchers("/api/chatbot/suggest-department").permitAll()
                .requestMatchers("/api/chatbot/submit-complaint").hasRole("CITIZEN")
                .requestMatchers("/api/chatbot/my-active-complaints").hasRole("CITIZEN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        http.addFilterBefore(
                new JwtAuthFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers("/ws/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}