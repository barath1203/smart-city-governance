package com.smartcity.governance.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

    // 🔹 1. Create Complaint (Citizen - JWT Based)
    @PostMapping("/create")
    public Complaint createComplaint(@RequestBody Complaint complaint,
                                     Principal principal) {

        // ✅ Get logged-in citizen from JWT
        User citizen = userRepository.findByEmail(principal.getName());
        complaint.setUser(citizen);

        // 🔥 Auto-Assign Officer Based on Department
        Optional<User> officer = userRepository
        	    .findFirstByRoleAndDepartment("OFFICER", complaint.getDepartment());

        if (officer.isPresent()) {
            complaint.setAssignedOfficer(officer.get());
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        } else {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        return complaintRepository.save(complaint);
    }

    // 🔹 2. Get All Complaints (Admin)
    @GetMapping("/all")
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // 🔹 3. Get Complaints of Logged-in Citizen
    @GetMapping("/my")
    public List<Complaint> getMyComplaints(Principal principal) {

        User citizen = userRepository.findByEmail(principal.getName());
        return complaintRepository.findByUser(citizen);
    }

    // 🔹 4. Officer: Get Assigned Complaints
    @GetMapping("/assigned")
    public List<Complaint> getAssignedComplaints(Principal principal) {

        User officer = userRepository.findByEmail(principal.getName());
        return complaintRepository.findByAssignedOfficer(officer);
    }

    // 🔹 5. Update Complaint Status (Officer/Admin)
    @PutMapping("/update-status/{complaintId}")
    public Complaint updateStatus(@PathVariable Long complaintId,
                                  @RequestParam ComplaintStatus status) {

        Complaint complaint = complaintRepository
                .findById(complaintId)
                .orElseThrow();

        complaint.setStatus(status);

        // 🔔 Notify citizen
        Notification notification = new Notification();
        notification.setMessage("Your complaint ID " + complaintId +
                " status changed to " + status);
        notification.setRole("CITIZEN");

        notificationRepository.save(notification);

        return complaintRepository.save(complaint);
    }

    // 🔹 6. Admin: Manual Assign Officer
    @PutMapping("/assign/{complaintId}")
    public Complaint assignComplaint(@PathVariable Long complaintId,
                                     @RequestParam Long officerId) {

        Complaint complaint = complaintRepository
                .findById(complaintId)
                .orElseThrow();

        User officer = userRepository
                .findById(officerId)
                .orElseThrow();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        return complaintRepository.save(complaint);
    }
}