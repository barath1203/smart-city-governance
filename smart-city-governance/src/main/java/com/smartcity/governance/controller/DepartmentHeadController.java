package com.smartcity.governance.controller;


import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.OfficerRating;
import com.smartcity.governance.model.RatingSource;
import com.smartcity.governance.model.Role;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.OfficerRatingRepository;
import com.smartcity.governance.repository.UserRepository;
import com.smartcity.governance.service.PerformanceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dh")
@CrossOrigin(origins = "*")
public class DepartmentHeadController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired private OfficerRatingRepository ratingRepository;
    @Autowired private PerformanceService performanceService;

    // ✅ 1. Create Officer (by DH)
    @PostMapping("/create-officer")
    public User createOfficer(@RequestBody User user, Authentication auth) {
        String email = auth.getName();
        User dh = userRepository.findByEmail(email);
        if (dh == null) throw new RuntimeException("DH not found");
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already exists");
        }
        user.setRole(Role.OFFICER);
        user.setDepartment(dh.getDepartment());
        user.setPassword(passwordEncoder.encode(user.getPassword())); // ← add this
        return userRepository.save(user);
    }

    // ✅ 2. View all complaints of DH department
    @GetMapping("/complaints")
    public List<Complaint> getDepartmentComplaints(Authentication auth) {

        String email = auth.getName();
        User dh = userRepository.findByEmail(email);

        return complaintRepository.findByDepartment(dh.getDepartment());
    }

    // ✅ 3. Reassign complaint (override auto-assign)
    @PutMapping("/reassign/{complaintId}")
    public Complaint reassignComplaint(
            @PathVariable Long complaintId,
            @RequestParam Long officerId,
            Authentication auth) {

        User dh = userRepository.findByEmail(auth.getName());

        Complaint complaint = complaintRepository
                .findById(complaintId)
                .orElseThrow();

        if (!complaint.getDepartment().equals(dh.getDepartment())) {
            throw new RuntimeException("Access denied");
        }

        User officer = userRepository
                .findById(officerId)
                .orElseThrow();

        if (!officer.getDepartment().equals(dh.getDepartment())) {
            throw new RuntimeException("Invalid officer");
        }

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setEscalated(false);

        return complaintRepository.save(complaint);
    }
    // ✅ 4. View all officers in DH department
    @GetMapping("/officers")
    public List<User> getDepartmentOfficers(Authentication auth) {

        String email = auth.getName();
        User dh = userRepository.findByEmail(email);

        return userRepository.findByDepartmentAndRole(
                dh.getDepartment(),
                Role.OFFICER
        );
    }

    // ✅ 5. (Optional) Add performance credit to officer
    @PutMapping("/add-credit/{officerId}")
    public User addCredit(
            @PathVariable Long officerId,
            @RequestParam int credit) {

        User officer = userRepository.findById(officerId).orElseThrow();

        // Example: assuming you have a performanceScore field
        int currentScore = officer.getPerformanceScore();
        officer.setPerformanceScore(currentScore + credit);

        return userRepository.save(officer);
    }
    
    @GetMapping("/escalated")
    public ResponseEntity<List<Complaint>> getEscalatedComplaints(Authentication auth) {
        User dh = userRepository.findByEmail(auth.getName());
        List<Complaint> escalated = complaintRepository
            .findByDepartmentAndEscalatedTrue(dh.getDepartment());
        return ResponseEntity.ok(escalated);
    }
    
    @PutMapping("/complaints/{id}/priority")
    public ResponseEntity<?> updatePriority(
            @PathVariable Long id,
            @RequestParam ComplaintPriority priority,
            Authentication auth) {

        User dh = userRepository.findByEmail(auth.getName());
        Complaint complaint = complaintRepository.findById(id).orElseThrow();

        // DH can only update complaints in their department
        if (!complaint.getDepartment().equals(dh.getDepartment())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        complaint.setPriority(priority);
        complaintRepository.save(complaint);
        return ResponseEntity.ok("Priority updated");
    }
    

    @PostMapping("/complaints/{id}/rate")
    public ResponseEntity<?> rateOfficer(
            @PathVariable Long id,
            @RequestParam int rating,
            @RequestParam(required = false) String feedback,
            Authentication auth) {

        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("Rating must be 1 to 5");
        }

        User dh = userRepository.findByEmail(auth.getName());
        Complaint complaint = complaintRepository.findById(id).orElseThrow();

        if (!complaint.getDepartment().equals(dh.getDepartment())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            return ResponseEntity.badRequest().body("Can only rate resolved complaints");
        }

        // Prevent duplicate rating
        if (ratingRepository
        	    .findByComplaintIdAndSource(id, RatingSource.DEPARTMENT_HEAD)
        	    .isPresent()) {
        	    return ResponseEntity.badRequest().body("Already rated");
        	}

        OfficerRating r = new OfficerRating();
        if (complaint.getAssignedOfficer() == null) {
            return ResponseEntity.badRequest().body("No officer assigned");
        }
        r.setOfficer(complaint.getAssignedOfficer());
        r.setComplaint(complaint);
        r.setRating(rating);
        r.setFeedback(feedback);
        r.setSource(RatingSource.DEPARTMENT_HEAD);
        ratingRepository.save(r);

        performanceService.recalculateScore(complaint.getAssignedOfficer());

        return ResponseEntity.ok("Rating submitted");
    }
}