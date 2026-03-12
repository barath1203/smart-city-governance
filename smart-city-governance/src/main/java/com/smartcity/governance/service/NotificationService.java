package com.smartcity.governance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Send to a specific user by email
    public void notifyUser(String email, String message) {
        messagingTemplate.convertAndSendToUser(
            email,
            "/queue/notifications",  // ← user-specific channel
            message
        );
    }

    // Broadcast to all admins
    public void notifyAdmins(String message) {
        messagingTemplate.convertAndSend("/topic/admin", message);
    }
}