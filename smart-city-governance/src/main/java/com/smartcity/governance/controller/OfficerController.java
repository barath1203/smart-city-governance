package com.smartcity.governance.controller;

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
import com.smartcity.governance.model.CoordinationRequest;
import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.Notification;
import com.smartcity.governance.model.RequestStatus;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.CoordinationRequestRepository;
import com.smartcity.governance.repository.NotificationRepository;
import com.smartcity.governance.repository.UserRepository;
import com.smartcity.governance.service.NotificationService;
import com.smartcity.governance.service.PerformanceService;

@RestController
@RequestMapping("/api/officer")
@CrossOrigin(origins = "*")
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

	// 🔹 1. Get complaints assigned to logged-in officer
	@GetMapping("/complaints")
	public List<Complaint> getOfficerComplaints(Authentication authentication) {
		String email = authentication.getName();
		User officer = userRepository.findByEmail(email);
		return complaintRepository.findByAssignedOfficer(officer);
	}

	// 🔹 2. Update Complaint Status + Notify Citizen

	@PutMapping("/update-status/{id}")
	public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam ComplaintStatus status) {

	    Complaint complaint = complaintRepository.findById(id).orElse(null);
	    if (complaint == null) {
	        return ResponseEntity.notFound().build();
	    }

	    complaint.setStatus(status);

	    if (status == ComplaintStatus.RESOLVED) {
	        complaint.setEscalated(false);
	        complaint.setResolvedAt(java.time.LocalDateTime.now());

	        // 🔥🔥🔥 MOST IMPORTANT LINE
	        performanceService.recalculateScore(complaint.getAssignedOfficer());
	    }

	    complaintRepository.save(complaint);

	    Notification notification = new Notification();
	    notification.setMessage("Your complaint '" + complaint.getTitle() + "' status has been updated to " + status);
	    notification.setRole("CITIZEN");
	    notification.setCreatedAt(java.time.LocalDateTime.now());
	    notificationRepository.save(notification);

	    String citizenEmail = complaint.getUser().getEmail();
	    notificationService.notifyUser(
	        citizenEmail,
	        "Your complaint '" + complaint.getTitle() + "' is now " + status
	    );

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