package com.smartcity.governance.controller;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.CoordinationAssignment;
import com.smartcity.governance.model.CoordinationRequest;
import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.Notification;
import com.smartcity.governance.model.RequestStatus;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.CoordinationAssignmentRepository;
import com.smartcity.governance.repository.CoordinationRequestRepository;
import com.smartcity.governance.repository.NotificationRepository;
import com.smartcity.governance.repository.UserRepository;
import com.smartcity.governance.service.NotificationService;
import com.smartcity.governance.service.PerformanceService;

@RestController
@RequestMapping("/api/officer")
public class OfficerController {

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PerformanceService performanceService;

	@Autowired
	private CoordinationRequestRepository coordinationRequestRepository;
	
	@Autowired
	private CoordinationAssignmentRepository coordinationAssignmentRepository;
	
	

	// 🔹 1. Get complaints assigned to logged-in officer
	@GetMapping("/complaints")
	public List<Complaint> getOfficerComplaints(Authentication authentication) {
	    String email = authentication.getName();
	    User officer = userRepository.findByEmail(email);

	    // Directly assigned complaints
	    List<Complaint> assigned = complaintRepository.findByAssignedOfficer(officer);

	    // Coordination-assisted complaints
	    List<Complaint> assisted = coordinationAssignmentRepository
	            .findByOfficerAndActiveTrue(officer)
	            .stream()
	            .map(CoordinationAssignment::getComplaint)
	            .toList();

	    // Merge without duplicates
	    List<Complaint> all = new java.util.ArrayList<>(assigned);
	    for (Complaint c : assisted) {
	        if (all.stream().noneMatch(x -> x.getId().equals(c.getId()))) {
	            all.add(c);
	        }
	    }

	    return all;
	}

	// 🔹 2. Update Complaint Status + Notify Citizen

	@PutMapping("/update-status/{id}")
	public ResponseEntity<?> updateStatus(@PathVariable Long id,
	                                       @RequestParam ComplaintStatus status,
	                                       Authentication authentication) {

	    Complaint complaint = complaintRepository.findById(id).orElse(null);
	    if (complaint == null) return ResponseEntity.notFound().build();

	    User officer = userRepository.findByEmail(authentication.getName());

	    boolean isPrimary = complaint.getAssignedOfficer() != null &&
	                        complaint.getAssignedOfficer().getId().equals(officer.getId());

	    if (isPrimary) {

	        if (status == ComplaintStatus.RESOLVED) {
	            // ✅ Check all assisting officers have resolved their part
	            List<CoordinationAssignment> assignments =
	                coordinationAssignmentRepository.findByComplaintAndActiveTrue(complaint);

	            boolean allAssistsResolved = assignments.stream()
	                .allMatch(a -> a.getAssistStatus() == ComplaintStatus.RESOLVED);

	            if (!allAssistsResolved && !assignments.isEmpty()) {
	                return ResponseEntity.badRequest().body(
	                    "Cannot resolve yet. Assisting officers have not completed their part."
	                );
	            }

	            // ✅ All assists done — resolve
	            complaint.setStatus(ComplaintStatus.RESOLVED);
	            complaint.setEscalated(false);
	            complaint.setResolvedAt(LocalDateTime.now());
	            performanceService.recalculateScore(officer);

	        } else {
	            complaint.setStatus(status);
	        }

	        complaintRepository.save(complaint);

	        // ✅ Notify citizen
	        Notification notification = new Notification();
	        notification.setMessage("Your complaint '" + complaint.getTitle() +
	                "' status has been updated to " + status);
	        notification.setUser(complaint.getUser());
	        notification.setCreatedAt(LocalDateTime.now());
	        notificationRepository.save(notification);

	        notificationService.notifyUser(
	            complaint.getUser().getEmail(),
	            "Your complaint '" + complaint.getTitle() + "' is now " + status
	        );

	    } else {
	        // ✅ Assisting officer — update only their CoordinationAssignment status
	        List<CoordinationAssignment> assignments =
	            coordinationAssignmentRepository.findByOfficerAndActiveTrue(officer);

	        CoordinationAssignment myAssignment = assignments.stream()
	            .filter(a -> a.getComplaint().getId().equals(complaint.getId()))
	            .findFirst()
	            .orElse(null);

	        if (myAssignment == null) {
	            return ResponseEntity.status(403).body("You are not assigned to this complaint");
	        }

	        myAssignment.setAssistStatus(status);
	        coordinationAssignmentRepository.save(myAssignment);

	        // ✅ Notify primary officer
	        Notification notification = new Notification();
	        notification.setMessage("Assisting Officer " + officer.getName() +
	            " marked their part as " + status +
	            " for complaint: " + complaint.getTitle());
	        notification.setUser(complaint.getAssignedOfficer());
	        notification.setCreatedAt(LocalDateTime.now());
	        notificationRepository.save(notification);

	        notificationService.notifyUser(
	            complaint.getAssignedOfficer().getEmail(),
	            notification.getMessage()
	        );
	    }

	    return ResponseEntity.ok("Status updated successfully");
	}

	// 🔹 3. Filter by Status
	@GetMapping("/complaints/status/{status}")
	public List<Complaint> getByStatus(@PathVariable ComplaintStatus status, Authentication authentication) {
		String email = authentication.getName();
		User officer = userRepository.findByEmail(email);
		return complaintRepository.findByAssignedOfficerAndStatus(officer, status);
	}

	// 🔹 4. Filter by Priority
	@GetMapping("/complaints/priority/{priority}")
	public List<Complaint> getByPriority(@PathVariable ComplaintPriority priority, Authentication authentication) {
		String email = authentication.getName();
		User officer = userRepository.findByEmail(email);
		return complaintRepository.findByAssignedOfficerAndPriority(officer, priority);
	}

	@GetMapping("/performance")
	public int getPerformance(Authentication auth) {
	    User officer = userRepository.findByEmail(auth.getName());
	    return officer.getPerformanceScore();
	}

	@PostMapping("/request-coordination/{complaintId}")
	public ResponseEntity<?> requestCoordination(
	        @PathVariable Long complaintId,
	        @RequestParam Department department,
	        @RequestParam String reason,
	        Authentication auth) {

	    User officer = userRepository.findByEmail(auth.getName());
	    Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();

	    CoordinationRequest req = new CoordinationRequest();
	    req.setComplaint(complaint);
	    req.setRequestedBy(officer);
	    req.setRequestedDepartment(department);
	    req.setReason(reason);
	    req.setStatus(RequestStatus.PENDING);

	    coordinationRequestRepository.save(req);
	    System.out.println("🔥 Coordination API HIT");

	    return ResponseEntity.ok("Request sent to admin");
	}
}