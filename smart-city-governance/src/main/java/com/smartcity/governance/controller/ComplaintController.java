package com.smartcity.governance.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createComplaint(
            @RequestBody Complaint complaint,
            Authentication authentication) {

        String email = authentication.getName();
        User citizen = userRepository.findByEmail(email);
        complaint.setUser(citizen);

        String dept = complaint.getDepartment().name();
        User officer = userRepository.findFirstByRoleAndDepartment("OFFICER", dept);

        if (officer != null) {
            complaint.setAssignedOfficer(officer);
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);

            Notification citizenNotif = new Notification();
            citizenNotif.setMessage("Your complaint '" + complaint.getTitle() +
                    "' has been auto-assigned to officer " + officer.getName() +
                    " from " + dept + " department.");
            citizenNotif.setRole("CITIZEN");
            notificationRepository.save(citizenNotif);

            Notification officerNotif = new Notification();
            officerNotif.setMessage("New complaint assigned to you: '" +
                    complaint.getTitle() + "' — Priority: " +
                    complaint.getPriority());
            officerNotif.setRole("OFFICER");
            notificationRepository.save(officerNotif);

        } else {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        Complaint saved = complaintRepository.save(complaint);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/my")
    public List<Complaint> getMyComplaints(Authentication authentication) {
        String email = authentication.getName();
        User citizen = userRepository.findByEmail(email);
        return complaintRepository.findByUser(citizen);
    }

    @GetMapping("/all")
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByPriorityDesc();
    }

    @PutMapping("/assign/{complaintId}")
    public ResponseEntity<?> assignComplaint(
            @PathVariable Long complaintId,
            @RequestParam Long officerId) {

        Complaint complaint = complaintRepository
                .findById(complaintId).orElseThrow();
        User officer = userRepository
                .findById(officerId).orElseThrow();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaintRepository.save(complaint);

        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' has been assigned to officer " + officer.getName());
        notif.setRole("CITIZEN");
        notificationRepository.save(notif);

        return ResponseEntity.ok("Officer assigned successfully");
    }

    @PutMapping("/update-status/{complaintId}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long complaintId,
            @RequestParam ComplaintStatus status) {

        Complaint complaint = complaintRepository
                .findById(complaintId).orElseThrow();
        complaint.setStatus(status);
        complaintRepository.save(complaint);

        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' status updated to " + status);
        notif.setRole("CITIZEN");
        notificationRepository.save(notif);

        return ResponseEntity.ok("Status updated to " + status);
    }
}