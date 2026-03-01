package com.smartcity.governance.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalComplaints", complaintRepository.count());
        stats.put("totalOfficers", userRepository.findByRole("OFFICER").size());
        stats.put("totalCitizens", userRepository.findByRole("CITIZEN").size());

        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("OPEN",        complaintRepository.countByStatus(ComplaintStatus.OPEN));
        statusStats.put("IN_PROGRESS", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        statusStats.put("RESOLVED",    complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.put("byStatus", statusStats);

        Map<String, Long> deptStats = new HashMap<>();
        deptStats.put("WATER",       complaintRepository.countByDepartment(Department.WATER));
        deptStats.put("ELECTRICITY", complaintRepository.countByDepartment(Department.ELECTRICITY));
        deptStats.put("SANITATION",  complaintRepository.countByDepartment(Department.SANITATION));
        deptStats.put("ROAD",        complaintRepository.countByDepartment(Department.ROAD));
        stats.put("byDepartment", deptStats);

        Map<String, Long> priorityStats = new HashMap<>();
        priorityStats.put("LOW",       complaintRepository.countByPriority(ComplaintPriority.LOW));
        priorityStats.put("MEDIUM",    complaintRepository.countByPriority(ComplaintPriority.MEDIUM));
        priorityStats.put("HIGH",      complaintRepository.countByPriority(ComplaintPriority.HIGH));
        priorityStats.put("EMERGENCY", complaintRepository.countByPriority(ComplaintPriority.EMERGENCY));
        stats.put("byPriority", priorityStats);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/all-complaints")
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByPriorityDesc();
    }

    @GetMapping("/officers")
    public List<User> getAllOfficers() {
        return userRepository.findByRole("OFFICER");
    }

    @PostMapping("/add-officer")
    public ResponseEntity<?> addOfficer(@RequestBody User user,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        user.setRole("OFFICER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Officer added successfully");
    }

    @PutMapping("/assign/{complaintId}")
    public ResponseEntity<?> assignComplaint(
            @PathVariable Long complaintId,
            @RequestParam Long officerId) {

        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        User officer = userRepository.findById(officerId).orElseThrow();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaintRepository.save(complaint);

        return ResponseEntity.ok("Officer assigned successfully");
    }
}