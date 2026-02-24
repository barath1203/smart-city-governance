package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;

@RestController
@RequestMapping("/api/officer")
@CrossOrigin(origins = "*")
public class OfficerController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔹 1. Get complaints assigned to logged-in officer
    @GetMapping("/complaints")
    public List<Complaint> getOfficerComplaints(Authentication authentication) {

        String email = authentication.getName();
        User officer = userRepository.findByEmail(email);

        // Return only complaints assigned to this officer
        return complaintRepository.findByAssignedOfficer(officer);
    }

    // 🔹 2. Update Complaint Status (ENUM based)
    @PutMapping("/update-status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestParam ComplaintStatus status) {

        Complaint complaint = complaintRepository.findById(id).orElse(null);

        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        complaint.setStatus(status);
        complaintRepository.save(complaint);

        return ResponseEntity.ok("Status updated successfully");
    }

    // 🔹 3. Filter by Status (Optional Advanced Feature)
    @GetMapping("/complaints/status/{status}")
    public List<Complaint> getByStatus(@PathVariable ComplaintStatus status,
                                       Authentication authentication) {

        String email = authentication.getName();
        User officer = userRepository.findByEmail(email);

        return complaintRepository
                .findByAssignedOfficerAndStatus(officer, status);
    }

    // 🔹 4. Filter by Priority (Optional Advanced Feature)
    @GetMapping("/complaints/priority/{priority}")
    public List<Complaint> getByPriority(@PathVariable ComplaintPriority priority,
                                         Authentication authentication) {

        String email = authentication.getName();
        User officer = userRepository.findByEmail(email);

        return complaintRepository
                .findByAssignedOfficerAndPriority(officer, priority);
    }
}