package com.smartcity.governance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.Notification;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EscalationScheduler {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // ✅ Runs every 30 minutes
    @Scheduled(fixedRate = 1800000)
    public void escalateOverdueComplaints() {
        List<Complaint> overdue = complaintRepository.findOverdueComplaints(LocalDateTime.now());

        for (Complaint complaint : overdue) {

            // 1. Change status to ESCALATED
            complaint.setStatus(ComplaintStatus.ESCALATED);
            complaint.setEscalated(true);
            complaintRepository.save(complaint);

            // 2. Notify ADMIN
            Notification adminNotif = new Notification();
            adminNotif.setRole("ADMIN");
            adminNotif.setMessage(
                "🚨 ESCALATED: Complaint #" + complaint.getId() +
                " '" + complaint.getTitle() + "' has exceeded its deadline. " +
                "Priority: " + complaint.getPriority()
            );
            adminNotif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(adminNotif);

            // 3. Notify CITIZEN
            Notification citizenNotif = new Notification();
            citizenNotif.setRole("CITIZEN");
            citizenNotif.setMessage(
                "⚠️ Your complaint '" + complaint.getTitle() +
                "' has been escalated to higher authority for faster resolution."
            );
            citizenNotif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(citizenNotif);
        }

        System.out.println("✅ Escalation check done. Escalated: " + overdue.size() + " complaints.");
    }
}