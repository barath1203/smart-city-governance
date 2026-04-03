package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email);
        List<Notification> notifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(notifications);
    }
}

