package com.smartcity.governance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintPriority;
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

    @Scheduled(fixedRate = 1800000)
    public void escalateOverdueComplaints() {
        List<Complaint> overdue = complaintRepository.findOverdueComplaints(LocalDateTime.now());

        for (Complaint complaint : overdue) {
            // 1. Escalate priority one step higher
            complaint.setPriority(escalatePriority(complaint.getPriority()));

            // 2. Change status
            complaint.setStatus(ComplaintStatus.ESCALATED);
            complaint.setEscalated(true);
            complaintRepository.save(complaint);

            // 3. Notify DEPARTMENT_HEAD instead of ADMIN
            Notification dhNotif = new Notification();
            dhNotif.setRole("DEPARTMENT_HEAD");
            dhNotif.setMessage(
                "🚨 ESCALATED: Complaint #" + complaint.getId() +
                " '" + complaint.getTitle() + "' has exceeded its deadline." +
                " Department: " + complaint.getDepartment() +
                " | New Priority: " + complaint.getPriority()
            );
            dhNotif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(dhNotif);

            // 4. Notify CITIZEN
            Notification citizenNotif = new Notification();
            citizenNotif.setRole("CITIZEN");
            citizenNotif.setMessage(
                "⚠️ Your complaint '" + complaint.getTitle() +
                "' has been escalated for faster resolution." +
                " Priority raised to: " + complaint.getPriority()
            );
            citizenNotif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(citizenNotif);
        }

        System.out.println("✅ Escalation check done. Escalated: " + overdue.size() + " complaints.");
    }

    private ComplaintPriority escalatePriority(ComplaintPriority current) {
        return switch (current) {
            case LOW       -> ComplaintPriority.MEDIUM;
            case MEDIUM    -> ComplaintPriority.HIGH;
            case HIGH      -> ComplaintPriority.EMERGENCY;
            case EMERGENCY -> ComplaintPriority.EMERGENCY;
        };
    }
}