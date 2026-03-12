package com.smartcity.governance.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;
import com.smartcity.governance.service.NotificationService;

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
    
    @Autowired
    private NotificationService notificationService;  // ← ADD

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
            citizenNotif.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(citizenNotif);

            Notification officerNotif = new Notification();
            officerNotif.setMessage("New complaint assigned to you: '" +
                    complaint.getTitle() + "' — Priority: " +
                    complaint.getPriority());
            officerNotif.setRole("OFFICER");
            officerNotif.setCreatedAt(java.time.LocalDateTime.now());
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

        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        User officer = userRepository.findById(officerId).orElseThrow();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setEscalated(false);
        complaintRepository.save(complaint);

        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' has been assigned to officer " + officer.getName());
        notif.setRole("CITIZEN");
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notif);

        return ResponseEntity.ok("Officer assigned successfully");
    }

    @PutMapping("/update-status/{complaintId}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long complaintId,
            @RequestParam ComplaintStatus status) {

        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        complaint.setStatus(status);

        if (status == ComplaintStatus.RESOLVED) {
            complaint.setEscalated(false);
        }

        complaintRepository.save(complaint);

        // ✅ Save to DB
        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' status updated to " + status);
        notif.setRole("CITIZEN");
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notif);

        // ⚡ WebSocket push
        String citizenEmail = complaint.getUser().getEmail();
        System.out.println("📡 Sending WebSocket to: " + citizenEmail);
        notificationService.notifyUser(
            citizenEmail,
            "Your complaint '" + complaint.getTitle() + "' is now " + status
        );

        return ResponseEntity.ok("Status updated to " + status);
    }

    // ✅ Citizen rates a resolved complaint
    @PutMapping("/rate/{complaintId}")
    public ResponseEntity<?> rateComplaint(
            @PathVariable Long complaintId,
            @RequestBody RatingRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User citizen = userRepository.findByEmail(email);

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        // ✅ Only complaint owner can rate
        if (!complaint.getUser().getId().equals(citizen.getId())) {
            return ResponseEntity.status(403).body("You can only rate your own complaints");
        }

        // ✅ Only resolved complaints can be rated
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            return ResponseEntity.badRequest().body("You can only rate resolved complaints");
        }

        // ✅ Prevent double rating
        if (complaint.isRated()) {
            return ResponseEntity.badRequest().body("You have already rated this complaint");
        }

        // ✅ Validate rating range
        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }

        complaint.setRating(request.getRating());
        complaint.setRatingComment(request.getRatingComment());
        complaint.setRated(true);
        complaintRepository.save(complaint);

        // ✅ Notify officer about the rating
        Notification notif = new Notification();
        notif.setMessage("⭐ Your resolved complaint '" + complaint.getTitle() +
                "' received a " + request.getRating() + "-star rating.");
        notif.setRole("OFFICER");
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notif);

        return ResponseEntity.ok("Rating submitted successfully");
    }

    // ✅ Get average rating for an officer
    @GetMapping("/officer-rating/{officerId}")
    public ResponseEntity<Map<String, Object>> getOfficerRating(
            @PathVariable Long officerId) {

        User officer = userRepository.findById(officerId).orElse(null);
        if (officer == null) return ResponseEntity.notFound().build();

        List<Complaint> rated = complaintRepository
                .findByAssignedOfficerAndRatedTrue(officer);

        double avg = rated.stream()
                .mapToInt(Complaint::getRating)
                .average()
                .orElse(0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("officerId", officerId);
        result.put("officerName", officer.getName());
        result.put("totalRated", rated.size());
        result.put("averageRating", Math.round(avg * 10.0) / 10.0);

        return ResponseEntity.ok(result);
    }
}