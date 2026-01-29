package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.Notification;
import com.smartcity.governance.repository.NotificationRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/{role}")
    public List<Notification> getNotifications(@PathVariable String role) {
        return notificationRepository.findByRole(role);
    }
}

