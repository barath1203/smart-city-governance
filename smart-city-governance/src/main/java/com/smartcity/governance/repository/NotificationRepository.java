package com.smartcity.governance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.smartcity.governance.model.Notification;

public interface NotificationRepository 
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRole(String role);
}

