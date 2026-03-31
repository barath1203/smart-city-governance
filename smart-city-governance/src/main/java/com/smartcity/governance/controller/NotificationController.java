package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.Notification;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.NotificationRepository;
import com.smartcity.governance.repository.UserRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my")
    public List<Notification> getMyNotifications(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        return notificationRepository.findByUser(user);
    }
}

