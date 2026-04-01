package com.smartcity.governance.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;
import com.smartcity.governance.service.NotificationService;

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
    
    @Autowired
    private CoordinationRequestRepository coordinationRequestRepository ;
    
    @Autowired
    private CoordinationAssignmentRepository coordinationAssignmentRepository ;
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalComplaints", complaintRepository.count());
        stats.put("totalOfficers", userRepository.findByRole(Role.OFFICER).size());
        stats.put("totalCitizens", userRepository.findByRole(Role.CITIZEN).size());

        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("OPEN",        complaintRepository.countByStatus(ComplaintStatus.OPEN));
        statusStats.put("IN_PROGRESS", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        statusStats.put("RESOLVED",    complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        statusStats.put("ESCALATED",   complaintRepository.countByStatus(ComplaintStatus.ESCALATED)); // ✅ NEW
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
        return userRepository.findByRole(Role.OFFICER);
    }


    @PostMapping("/add-officer")
    public ResponseEntity<?> addOfficer(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        if (user.getDepartment() == null) {
            return ResponseEntity.badRequest().body("Department is required");
        }
        user.setRole(Role.OFFICER);
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
        complaint.setEscalated(false); // ✅ Reset escalation when manually reassigned
        complaintRepository.save(complaint);

        // ✅ Notify officer
        Notification notif = new Notification();
        notif.setMessage("New complaint assigned to you: '" +
                complaint.getTitle() + "' — Priority: " + complaint.getPriority());
        notif.setRole("OFFICER");
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notif);

        return ResponseEntity.ok("Officer assigned successfully");
    }
    
    @PostMapping("/create-dh")
    public ResponseEntity<?> createDepartmentHead(@RequestBody User user) {

        // ✅ Check duplicate email
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // ✅ Must assign department
        if (user.getDepartment() == null) {
            return ResponseEntity.badRequest().body("Department is required for DH");
        }

        user.setRole(Role.DEPARTMENT_HEAD);
        user.setPassword(passwordEncoder.encode(user.getPassword())); // ✅ Encode password

        userRepository.save(user);
        return ResponseEntity.ok("Department head added successfully");
    }
    
    @GetMapping("/coordination-requests")
    public List<CoordinationRequest> getPendingRequests() {
        return coordinationRequestRepository.findByStatus(RequestStatus.PENDING);
    }
    
    @PostMapping("/approve-request/{id}")
    public ResponseEntity<?> approveRequest(@PathVariable Long id) {

        CoordinationRequest req = coordinationRequestRepository.findById(id).orElseThrow();
        Complaint complaint = req.getComplaint();

        // 🔍 Find officer
        User officer = userRepository.findFirstByRoleAndDepartment(
                Role.OFFICER, req.getRequestedDepartment()
        );

        if (officer != null) {

            // ✅ Create assignment
            CoordinationAssignment assignment = new CoordinationAssignment();
            assignment.setComplaint(complaint);
            assignment.setOfficer(officer);
            assignment.setDepartment(req.getRequestedDepartment());
            coordinationAssignmentRepository.save(assignment);

            // 🔔 Notify assisting officer
            Notification n1 = new Notification();
            n1.setMessage("You are assigned to assist complaint: " + complaint.getTitle());
            n1.setUser(officer);   // ✅ FIX
            n1.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n1);

            notificationService.notifyUser(officer.getEmail(), n1.getMessage());

            // 🔔 Notify requesting officer
            Notification n2 = new Notification();
            n2.setMessage("Your coordination request approved. Officer "
                    + officer.getName() + " is assisting.");
            n2.setUser(req.getRequestedBy());   // ✅ FIX
            n2.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n2);

            notificationService.notifyUser(
                    req.getRequestedBy().getEmail(),
                    n2.getMessage()
            );

        } else {
            req.setCoordinationOfficerPending(true);
        }

        // ✅ Update departments
        complaint.getDepartments().add(req.getRequestedDepartment());
        complaintRepository.save(complaint);

        // 🔔 Notify citizen
        Notification n3 = new Notification();
        n3.setMessage("Additional department assigned for faster resolution");
        n3.setUser(complaint.getUser());   // ✅ FIX
        n3.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n3);

        notificationService.notifyUser(
                complaint.getUser().getEmail(),
                n3.getMessage()
        );

        req.setStatus(RequestStatus.APPROVED);
        coordinationRequestRepository.save(req);

        return ResponseEntity.ok("Approved");
    }
    
    @PostMapping("/reject-request/{id}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {

        CoordinationRequest req = coordinationRequestRepository.findById(id).orElseThrow();

        req.setStatus(RequestStatus.REJECTED);
        coordinationRequestRepository.save(req);

        // Notify requesting officer
        notificationService.notifyUser(
            req.getRequestedBy().getEmail(),
            "Your coordination request was rejected"
        );

        return ResponseEntity.ok("Rejected");
    }
 
}