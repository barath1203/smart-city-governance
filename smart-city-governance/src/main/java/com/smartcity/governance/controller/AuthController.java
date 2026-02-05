package com.smartcity.governance.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

    	User dbUser = userRepository.findByEmail(user.getEmail());

    	if (dbUser == null ||
    	    !passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
    	    return ResponseEntity.status(401).body("Invalid credentials");
    	}


        Map<String, String> response = new HashMap<>();
        response.put("role", dbUser.getRole());
        response.put("name", dbUser.getName());

        return ResponseEntity.ok(response);
    }
}
