package com.smartcity.governance.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        System.out.println(">>> JwtAuthFilter triggered for: " + request.getMethod() + " " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println(">>> No Bearer token found — skipping auth");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            String role  = jwtService.extractRole(token);

            System.out.println(">>> email from token:  [" + email + "]");
            System.out.println(">>> role from token:   [" + role + "]");
            System.out.println(">>> token valid:       [" + jwtService.isTokenValid(token) + "]");
            System.out.println(">>> auth already set:  [" + (SecurityContextHolder.getContext().getAuthentication() != null) + "]");

            if (email != null
                    && jwtService.isTokenValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (role == null) {
                    System.out.println(">>> WARNING: role is null — cannot set authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                String springRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                System.out.println(">>> springRole set:    [" + springRole + "]");

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(springRole))
                    );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println(">>> Authentication set successfully for: " + email);

            } else {
                System.out.println(">>> Auth NOT set — reason:");
                System.out.println("    email null?          " + (email == null));
                System.out.println("    token valid?         " + jwtService.isTokenValid(token));
                System.out.println("    auth already exists? " + (SecurityContextHolder.getContext().getAuthentication() != null));
            }

        } catch (Exception e) {
            System.out.println(">>> JWT Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}