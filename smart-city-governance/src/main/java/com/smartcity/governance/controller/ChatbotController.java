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
        Department dept = departmentRouter.route(text);
        return ResponseEntity.ok(Map.of("department", dept.name()));
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
            @RequestBody Map<String, String> body,
            Authentication auth) {

        User citizen = userRepository.findByEmail(auth.getName());

        Complaint c = new Complaint();
        c.setTitle(body.get("title"));
        c.setDescription(body.get("description"));
        c.setLocation(body.get("location"));
        c.setDepartment(Department.valueOf(body.get("department")));
        c.setPriority(ComplaintPriority.valueOf(
            body.getOrDefault("priority", "LOW")));
        c.setUser(citizen);

        // auto-assign officer same as create flow
        User officer = userRepository.findFirstByRoleAndDepartment(
            Role.OFFICER, c.getDepartment());
        if (officer != null) {
            c.setAssignedOfficer(officer);
            c.setStatus(ComplaintStatus.IN_PROGRESS);
        } else {
            c.setStatus(ComplaintStatus.OPEN);
        }

        Complaint saved = complaintRepository.save(c);
        return ResponseEntity.ok(Map.of(
            "id",      saved.getId(),
            "title",   saved.getTitle(),
            "status",  saved.getStatus(),
            "department", saved.getDepartment()
        ));
    }
}