package com.smartcity.governance.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.Notification;
import com.smartcity.governance.model.OfficerRating;
import com.smartcity.governance.model.RatingRequest;
import com.smartcity.governance.model.RatingSource;
import com.smartcity.governance.model.Role;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.NotificationRepository;
import com.smartcity.governance.repository.OfficerRatingRepository;
import com.smartcity.governance.repository.UserRepository;
import com.smartcity.governance.service.NotificationService;
import com.smartcity.governance.service.PerformanceService;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired private OfficerRatingRepository ratingRepository;
    @Autowired private PerformanceService performanceService;


    @PostMapping("/create")
    public ResponseEntity<?> createComplaint(
            @RequestBody Complaint complaint,
            Authentication authentication) {

        String email = authentication.getName();
        User citizen = userRepository.findByEmail(email);
        complaint.setUser(citizen);
        
     // ✅ Set single imageUrl from the list (for display in existing UI)
        if (complaint.getImageUrls() != null && !complaint.getImageUrls().isEmpty()) {
            complaint.setImageUrl(complaint.getImageUrls().get(0));
        }

        String dept = complaint.getDepartment().name();
        User officer = userRepository.findFirstByRoleAndDepartment(Role.OFFICER, complaint.getDepartment());
        complaint.getDepartments().add(complaint.getDepartment());

        if (officer != null) {
            complaint.setAssignedOfficer(officer);
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);

            Notification citizenNotif = new Notification();
            citizenNotif.setMessage("Your complaint '" + complaint.getTitle() +
                    "' has been auto-assigned to officer " + officer.getName() +
                    " from " + dept + " department.");
            citizenNotif.setRole("CITIZEN");
            citizenNotif.setUser(complaint.getUser());
            citizenNotif.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(citizenNotif);

            Notification officerNotif = new Notification();
            officerNotif.setMessage("New complaint assigned to you: '" +
                    complaint.getTitle() + "' — Priority: " +
                    complaint.getPriority());
            officerNotif.setRole("OFFICER");
            officerNotif.setUser(officer);
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

    	Complaint complaint = complaintRepository.findById(complaintId)
    		    .orElseThrow(() -> new RuntimeException("Complaint not found"));
        User officer = userRepository.findById(officerId).orElseThrow();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setEscalated(false);
        complaintRepository.save(complaint);

        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' has been assigned to officer " + officer.getName());
        notif.setRole("CITIZEN");
        notif.setUser(complaint.getUser());
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
            complaint.setResolvedAt(LocalDateTime.now()); // ADD THIS
            complaint.setEscalated(false);
            performanceService.recalculateScore(complaint.getAssignedOfficer()); // ADD THIS
        }

        complaintRepository.save(complaint);

        // ✅ Save to DB
        Notification notif = new Notification();
        notif.setMessage("Your complaint '" + complaint.getTitle() +
                "' status updated to " + status);
        notif.setRole("CITIZEN");
        notif.setUser(complaint.getUser());
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

        if (!complaint.getUser().getId().equals(citizen.getId())) {
			return ResponseEntity.status(403).body("You can only rate your own complaints");
		}

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
			return ResponseEntity.badRequest().body("You can only rate resolved complaints");
		}

        if (complaint.isRated()) {
			return ResponseEntity.badRequest().body("You have already rated this complaint");
		}

        if (request.getRating() < 1 || request.getRating() > 5) {
			return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
		}

        // Existing logic — unchanged
        complaint.setRating(request.getRating());
        complaint.setRatingComment(request.getRatingComment());
        complaint.setRated(true);
        complaintRepository.save(complaint);

        // ← ADD: save to OfficerRating + recalculate score
        if (complaint.getAssignedOfficer() != null) {
            ratingRepository.findByComplaintIdAndSource(complaintId, RatingSource.CITIZEN)
                .ifPresentOrElse(
                    existing -> {},  // already rated, skip
                    () -> {
                        OfficerRating r = new OfficerRating();
                        r.setOfficer(complaint.getAssignedOfficer());
                        r.setComplaint(complaint);
                        r.setRating(request.getRating());
                        r.setFeedback(request.getRatingComment());
                        r.setSource(RatingSource.CITIZEN);
                        ratingRepository.save(r);
                        performanceService.recalculateScore(complaint.getAssignedOfficer());
                    }
                );
        }

        // Existing notification — unchanged
        Notification notif = new Notification();
        notif.setMessage("⭐ Your resolved complaint '" + complaint.getTitle() +
                "' received a " + request.getRating() + "-star rating.");
        notif.setRole("OFFICER");
        notif.setUser(complaint.getAssignedOfficer());
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notif);

        return ResponseEntity.ok("Rating submitted successfully");
    }
    // ✅ Get average rating for an officer
    @GetMapping("/officer-rating/{officerId}")
    public ResponseEntity<?> getOfficerRating(@PathVariable Long officerId) {
        User officer = userRepository.findById(officerId).orElse(null);
        if (officer == null) return ResponseEntity.notFound().build();

        List<Complaint> assigned = complaintRepository.findByAssignedOfficer(officer);
        int total    = assigned.size();
        int resolved = (int) assigned.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        int escalated = (int) assigned.stream()
            .filter(c -> c.isEscalated()).count();
        long onTimeResolved = assigned.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED
                      && c.getResolvedAt() != null
                      && c.getDeadline()   != null
                      && c.getResolvedAt().isBefore(c.getDeadline()))
            .count();

        List<OfficerRating> dhRatings      = ratingRepository.findByOfficerAndSource(officer, RatingSource.DEPARTMENT_HEAD);
        List<OfficerRating> citizenRatings = ratingRepository.findByOfficerAndSource(officer, RatingSource.CITIZEN);

        double dhAvg      = dhRatings.stream().mapToInt(OfficerRating::getRating).average().orElse(0.0);
        double citizenAvg = citizenRatings.stream().mapToInt(OfficerRating::getRating).average().orElse(0.0);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("officerId",       officer.getId());
        res.put("officerName",     officer.getName());
        res.put("performanceScore",officer.getPerformanceScore());
        res.put("total",           total);           // ← NEW
        res.put("resolved",        resolved);        // ← NEW
        res.put("escalated",       escalated);       // ← NEW
        res.put("onTimeResolved",  onTimeResolved);  // ← NEW
        res.put("citizenAvg",      citizenAvg);
        res.put("citizenRatings",  citizenRatings.size());
        res.put("dhAvg",           dhAvg);
        res.put("dhRatings",       dhRatings.size());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/request-coordination/{complaintId}")
    public ResponseEntity<?> requestCoordination(
            @PathVariable Long complaintId,
            @RequestParam Department department,
            Authentication authentication) {

        String email = authentication.getName();
        User officer = userRepository.findByEmail(email);
        User admin = userRepository.findFirstByRole(Role.ADMIN);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        // mark as escalated
        complaint.setEscalated(true);

        // 🔥 ADD second department (if using list)
        complaint.getDepartments().add(department);

        complaintRepository.save(complaint);

        // notify admin
        Notification notif = new Notification();
        notif.setMessage("⚠️ Coordination requested for complaint '"
                + complaint.getTitle() + "' to " + department);
        notif.setRole("ADMIN");
        notif.setUser(admin);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        return ResponseEntity.ok("Coordination request sent");
    }
}