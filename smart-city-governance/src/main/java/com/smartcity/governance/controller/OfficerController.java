package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.UserRepository;

@RestController
@RequestMapping("/api/officer")
public class OfficerController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    // View complaints assigned to officer
    @GetMapping("/complaints/{officerId}")
    public List<Complaint> getAssignedComplaints(@PathVariable Long officerId) {
        User officer = userRepository.findById(officerId).orElseThrow();
        return complaintRepository.findByAssignedOfficer(officer);
    }
    
    @GetMapping("/complaints")
    public List<Complaint> getOfficerComplaints(Authentication authentication) {

        String email = authentication.getName();
        User officer = userRepository.findByEmail(email);

        return complaintRepository.findByDepartment(officer.getDepartment());
    }
    
    @PutMapping("/update-status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestParam String status) {

        Complaint complaint = complaintRepository.findById(id).orElse(null);

        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        complaint.setStatus(status);
        complaintRepository.save(complaint);

        return ResponseEntity.ok("Status updated");
    }


}
