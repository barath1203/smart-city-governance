package com.smartcity.governance.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ComplaintRepository complaintRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 Dashboard Analytics
    @GetMapping("/dashboard")
    public Map<String, Long> dashboardStats() {

        Map<String, Long> stats = new HashMap<>();

        stats.put("TOTAL_COMPLAINTS", complaintRepository.count());
        stats.put("OPEN", complaintRepository.countByStatus("OPEN"));
        stats.put("IN_PROGRESS", complaintRepository.countByStatus("IN_PROGRESS"));
        stats.put("RESOLVED", complaintRepository.countByStatus("RESOLVED"));

        stats.put("WATER", complaintRepository.countByDepartment("WATER"));
        stats.put("ELECTRICITY", complaintRepository.countByDepartment("ELECTRICITY"));
        stats.put("ROAD", complaintRepository.countByDepartment("ROAD"));
        stats.put("SANITATION", complaintRepository.countByDepartment("SANITATION"));

        return stats;
    }
    @GetMapping("/all")
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }
    
    @GetMapping("/count")
    public long getComplaintCount() {
        return complaintRepository.count();
    }
    
    @GetMapping("/count-by-status")
    public Map<String, Long> countByStatus() {

        Map<String, Long> result = new HashMap<>();

        result.put("OPEN", complaintRepository.countByStatus("OPEN"));
        result.put("IN_PROGRESS", complaintRepository.countByStatus("IN_PROGRESS"));
        result.put("RESOLVED", complaintRepository.countByStatus("RESOLVED"));

        return result;
    }
    
    @GetMapping("/count-by-department")
    public Map<String, Long> countByDepartment() {

        Map<String, Long> result = new HashMap<>();

        result.put("WATER", complaintRepository.countByDepartment("WATER"));
        result.put("ELECTRICITY", complaintRepository.countByDepartment("ELECTRICITY"));
        result.put("ROAD", complaintRepository.countByDepartment("ROAD"));
        result.put("SANITATION", complaintRepository.countByDepartment("SANITATION"));

        return result;
    }
    
    @PostMapping("/add-officer")
    public ResponseEntity<?> addOfficer(@RequestBody User user) {

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setRole("OFFICER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("Officer added successfully");
    }
}
