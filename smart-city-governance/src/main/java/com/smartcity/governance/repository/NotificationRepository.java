package com.smartcity.governance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcity.governance.model.Notification;
import com.smartcity.governance.model.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRole(String role);
    List<Notification> findByUser(User user);
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}