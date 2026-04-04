package com.smartcity.governance.controller;

import com.smartcity.governance.model.*;
import com.smartcity.governance.repository.*;
import com.smartcity.governance.service.DepartmentRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FaqRepository faqRepository;
    @Autowired private DepartmentRouter departmentRouter;
    @Autowired private NotificationRepository notificationRepository;

    // Status tracker
    @GetMapping("/status/{complaintId}")
    public ResponseEntity<?> getStatus(@PathVariable Long complaintId) {
        return complaintRepository.findById(complaintId)
            .map(c -> {
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("id",          c.getId());
                res.put("title",       c.getTitle());
                res.put("status",      c.getStatus());
                res.put("priority",    c.getPriority());
                res.put("department",  c.getDepartment());
                res.put("assignedOfficer",
                    c.getAssignedOfficer() != null
                        ? c.getAssignedOfficer().getName() : "Unassigned");
                res.put("createdAt",   c.getCreatedAt());
                res.put("escalated",   c.isEscalated());
                return ResponseEntity.ok(res);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Department suggestion
    @GetMapping("/suggest-department")
    public ResponseEntity<?> suggestDepartment(@RequestParam String text) {
        DepartmentRouter.CategorizationResult result = departmentRouter.categorize(text);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("department",        result.department().name());
        response.put("priority",          result.priority().name());
        response.put("confidence",        result.confidence());
        response.put("conflict",          result.conflict());
        response.put("sensitiveLocation", result.sensitiveLocation());

        return ResponseEntity.ok(response);
    }

    // FAQ keyword match
    @GetMapping("/faq")
    public ResponseEntity<?> getFaq(@RequestParam String query) {
        String lower = query.toLowerCase();
        List<FaqEntry> all = faqRepository.findAll();

        FaqEntry best = all.stream()
            .filter(f -> f.getKeywords().stream()
                .filter(k -> lower.contains(k.toLowerCase()))
                .count() >= 2)
            .findFirst()
            .orElse(null);

        if (best != null) {
            return ResponseEntity.ok(Map.of("answer", best.getAnswer()));
        }

        // single keyword match fallback
        FaqEntry fallback = all.stream()
            .filter(f -> f.getKeywords().stream()
                .anyMatch(k -> lower.contains(k.toLowerCase())))
            .findFirst()
            .orElse(null);

        if (fallback != null) {
            return ResponseEntity.ok(Map.of("answer", fallback.getAnswer()));
        }

        return ResponseEntity.ok(Map.of(
            "answer",
            "I couldn't find an answer. Please contact support or raise a complaint."
        ));
    }

    // Submit complaint from bot
    @PostMapping("/submit-complaint")
    public ResponseEntity<?> submitComplaint(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        User citizen = userRepository.findByEmail(auth.getName());
        if (citizen == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        // Build complaint the same way ComplaintController does
        Complaint complaint = new Complaint();
        complaint.setTitle(body.get("title").toString());
        complaint.setDescription(body.get("description").toString());
        complaint.setLocation(body.getOrDefault("location", "").toString());
        complaint.setDepartment(Department.valueOf(body.get("department").toString()));
        complaint.setPriority(ComplaintPriority.valueOf(
            body.getOrDefault("priority", "LOW").toString()));
        complaint.setUser(citizen);

        // Coordinates
        try {
            Object lat = body.get("latitude");
            Object lng = body.get("longitude");
            if (lat != null && lng != null) {
                complaint.setLatitude(Double.parseDouble(lat.toString()));
                complaint.setLongitude(Double.parseDouble(lng.toString()));
            }
        } catch (NumberFormatException ignored) {}

        // ✅ Same auto-assign logic as ComplaintController
        String dept = complaint.getDepartment().name();
        User officer = userRepository.findFirstByRoleAndDepartment(
            Role.OFFICER, complaint.getDepartment());

        complaint.getDepartments().add(complaint.getDepartment());

        if (officer != null) {
            complaint.setAssignedOfficer(officer);
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);

            Notification citizenNotif = new Notification();
            citizenNotif.setMessage("Your complaint '" + complaint.getTitle() +
                "' has been auto-assigned to officer " + officer.getName() +
                " from " + dept + " department.");
            citizenNotif.setRole("CITIZEN");
            citizenNotif.setUser(citizen);
            citizenNotif.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(citizenNotif);

            Notification officerNotif = new Notification();
            officerNotif.setMessage("New complaint assigned to you: '" +
                complaint.getTitle() + "' — Priority: " + complaint.getPriority());
            officerNotif.setRole("OFFICER");
            officerNotif.setUser(officer);
            officerNotif.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(officerNotif);

        } else {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        Complaint saved = complaintRepository.save(complaint);

        return ResponseEntity.ok(Map.of(
            "id",         saved.getId(),
            "title",      saved.getTitle(),
            "status",     saved.getStatus().name(),
            "department", saved.getDepartment().name(),
            "officer",    officer != null ? officer.getName() : "Unassigned"
        ));
    }}