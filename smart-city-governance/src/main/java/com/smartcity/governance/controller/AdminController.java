package com.smartcity.governance.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.Department;
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

    @GetMapping("/dashboard")
    public Map<String, Long> dashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL_COMPLAINTS", complaintRepository.count());
        // ✅ Pass enum values, not Strings
        stats.put("OPEN",        complaintRepository.countByStatus(ComplaintStatus.OPEN));
        stats.put("IN_PROGRESS", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.put("RESOLVED",    complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.put("WATER",       complaintRepository.countByDepartment(Department.WATER));
        stats.put("ELECTRICITY", complaintRepository.countByDepartment(Department.ELECTRICITY));
        stats.put("ROAD",        complaintRepository.countByDepartment(Department.ROAD));
        stats.put("SANITATION",  complaintRepository.countByDepartment(Department.SANITATION));
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
        result.put("OPEN",        complaintRepository.countByStatus(ComplaintStatus.OPEN));
        result.put("IN_PROGRESS", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        result.put("RESOLVED",    complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        return result;
    }

    @GetMapping("/count-by-department")
    public Map<String, Long> countByDepartment() {
        Map<String, Long> result = new HashMap<>();
        result.put("WATER",       complaintRepository.countByDepartment(Department.WATER));
        result.put("ELECTRICITY", complaintRepository.countByDepartment(Department.ELECTRICITY));
        result.put("ROAD",        complaintRepository.countByDepartment(Department.ROAD));
        result.put("SANITATION",  complaintRepository.countByDepartment(Department.SANITATION));
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