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
public class ChatbotController {

	@Autowired
	private ComplaintRepository complaintRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private FaqRepository faqRepository;
	@Autowired
	private DepartmentRouter departmentRouter;
	@Autowired
	private NotificationRepository notificationRepository;

	// ── Department label → Enum map ──────────────
	private static final Map<String, Department> DEPT_MAP = new LinkedHashMap<>();
	static {
		DEPT_MAP.put("Water", Department.WATER);
		DEPT_MAP.put("Road", Department.ROAD);
		DEPT_MAP.put("Electricity", Department.ELECTRICITY);
		DEPT_MAP.put("Sanitation", Department.SANITATION);
		// Also support direct enum names as fallback
		DEPT_MAP.put("WATER", Department.WATER);
		DEPT_MAP.put("ROAD", Department.ROAD);
		DEPT_MAP.put("ELECTRICITY", Department.ELECTRICITY);
		DEPT_MAP.put("SANITATION", Department.SANITATION);
	}

	// ──────────────────────────────────────────────
	// Get active complaints of logged-in citizen
	// ──────────────────────────────────────────────
	@GetMapping("/my-active-complaints")
	public ResponseEntity<?> getActiveComplaints(Authentication auth) {
		if (auth == null) {
			return ResponseEntity.status(401).body("Unauthorized");
		}

		User citizen = userRepository.findByEmail(auth.getName());
		if (citizen == null) {
			return ResponseEntity.status(404).body("User not found");
		}

		List<Complaint> activeComplaints = complaintRepository.findByUserAndStatusNot(citizen,
				ComplaintStatus.RESOLVED);

		List<Map<String, Object>> result = new ArrayList<>();
		for (Complaint c : activeComplaints) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("id", c.getId());
			map.put("title", c.getTitle());
			map.put("status", c.getStatus());
			result.add(map);
		}

		return ResponseEntity.ok(result);
	}

	// ──────────────────────────────────────────────
	// Status tracker
	// ──────────────────────────────────────────────
	@GetMapping("/status/{complaintId}")
	public ResponseEntity<?> getStatus(@PathVariable Long complaintId, Authentication auth) {

		if (auth == null) {
			return ResponseEntity.status(401).body("Unauthorized");
		}

		User citizen = userRepository.findByEmail(auth.getName());
		if (citizen == null) {
			return ResponseEntity.status(404).body("User not found");
		}

		return complaintRepository.findByIdAndUser(complaintId, citizen).map(c -> {
			Map<String, Object> res = new LinkedHashMap<>();
			res.put("id", c.getId());
			res.put("title", c.getTitle());
			res.put("status", c.getStatus());
			res.put("priority", c.getPriority());
			res.put("department", c.getDepartment());
			res.put("assignedOfficer",
					c.getAssignedOfficer() != null ? c.getAssignedOfficer().getName() : "Unassigned");
			res.put("createdAt", c.getCreatedAt());
			res.put("escalated", c.isEscalated());
			return ResponseEntity.ok(res);
		}).orElse(ResponseEntity.notFound().build());
	}

	// ──────────────────────────────────────────────
	// Department suggestion
	// ──────────────────────────────────────────────
	@GetMapping("/suggest-department")
	public ResponseEntity<?> suggestDepartment(@RequestParam String text) {
		System.out.println("Received text: " + text);
		DepartmentRouter.CategorizationResult result = departmentRouter.categorize(text);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("department", result.department().name());
		response.put("priority", result.priority().name());
		response.put("confidence", result.confidence());
		response.put("conflict", result.conflict());
		response.put("sensitiveLocation", result.sensitiveLocation());

		return ResponseEntity.ok(response);
	}

	// ──────────────────────────────────────────────
	// FAQ
	// ──────────────────────────────────────────────
	@GetMapping("/faq")
	public ResponseEntity<?> getFaq(@RequestParam String query) {

		// Detect if query is Tamil (Unicode range 0B80–0BFF)
		boolean isTamil = query.chars().anyMatch(c -> c >= 0x0B80 && c <= 0x0BFF);

		String translated = translateToEnglish(query.toLowerCase().trim());
		String lower = isTamil ? translated : query.toLowerCase().trim();

		System.out.println("FAQ query: " + query);
		System.out.println("Translated: " + lower + " | Tamil: " + isTamil);

		List<FaqEntry> all = faqRepository.findAll();

		FaqEntry best = all.stream().map(f -> {
			long hits = f.getKeywords().stream().filter(k -> lower.contains(k.toLowerCase())).count();
			return Map.entry(f, hits);
		}).filter(e -> e.getValue() >= 1).max(Comparator.comparingLong(Map.Entry::getValue)).map(Map.Entry::getKey)
				.orElse(null);

		if (best != null) {
			// Return Tamil answer if query was Tamil and Tamil answer exists
			String answer = (isTamil && best.getAnswerTa() != null && !best.getAnswerTa().isBlank())
					? best.getAnswerTa()
					: best.getAnswer();
			return ResponseEntity.ok(Map.of("answer", answer));
		}

		String notFound = isTamil
				? "🤔 பதில் கண்டறிய முடியவில்லை. புகார் நிலை, தீர்வு நேரம், துறைகள், புகார் பதிவு பற்றி கேளுங்கள்."
				: "🤔 Could not find an answer. Try asking about: complaint status, resolution time, departments, or how to file a complaint.";

		return ResponseEntity.ok(Map.of("answer", notFound));
	}

	// ── Tamil Unicode → English keyword map ──────────────────────────────────────
	private String translateToEnglish(String text) {
		Map<String, String> tamilMap = new LinkedHashMap<>();

		// Time / resolution
		tamilMap.put("எவ்வளவு நேரம்", "how long");
		tamilMap.put("எவ்வளவு நேரத்தில்", "how long time");
		tamilMap.put("எத்தனை நாள்", "how many days");
		tamilMap.put("எத்தனை நாட்கள்", "days");
		tamilMap.put("நேரம்", "time");
		tamilMap.put("நாள்", "days");
		tamilMap.put("தீர்க்கப்படும்", "resolve resolved");
		tamilMap.put("தீர்வு", "resolve");
		tamilMap.put("எப்போது", "when");

		// Filing
		tamilMap.put("புகார் பதிவு", "file complaint register");
		tamilMap.put("புகார்", "complaint");
		tamilMap.put("பதிவு செய்", "register file");
		tamilMap.put("சமர்ப்பி", "submit");
		tamilMap.put("எப்படி", "how");

		// Tracking
		tamilMap.put("நிலை", "status track");
		tamilMap.put("கண்காணி", "track");
		tamilMap.put("சரிபார்", "check");

		// Departments
		tamilMap.put("துறை", "department");
		tamilMap.put("தண்ணீர்", "water");
		tamilMap.put("சாலை", "road");
		tamilMap.put("மின்சாரம்", "electricity");
		tamilMap.put("சுகாதாரம்", "sanitation");

		// Priority
		tamilMap.put("முன்னுரிமை", "priority");
		tamilMap.put("அவசரம்", "emergency urgent");

		// Escalation
		tamilMap.put("அதிகரிப்பு", "escalation escalated");
		tamilMap.put("மேல்முறையீடு", "escalation");

		// Officer
		tamilMap.put("அதிகாரி", "officer assigned");
		tamilMap.put("யார்", "who");

		// Offline
		tamilMap.put("இணையம் இல்லாமல்", "offline without internet");
		tamilMap.put("இணையம்", "internet");
		tamilMap.put("ஆஃப்லைன்", "offline");

		// Rating
		tamilMap.put("மதிப்பீடு", "rating rate");
		tamilMap.put("நட்சத்திரம்", "star");
		tamilMap.put("கருத்து", "feedback");

		// Language
		tamilMap.put("தமிழ்", "tamil language");
		tamilMap.put("குரல்", "voice speak");

		String result = text;
		for (Map.Entry<String, String> entry : tamilMap.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	// ──────────────────────────────────────────────
	// Submit complaint from bot
	// ──────────────────────────────────────────────
	@PostMapping("/submit-complaint")
	public ResponseEntity<?> submitComplaint(@RequestBody Map<String, Object> body, Authentication auth) {

		System.out.println("=== CHATBOT SUBMIT HIT ===");
		System.out.println("Auth: " + (auth != null ? auth.getName() : "NULL"));
		System.out.println("Body: " + body);

		if (auth == null || auth.getName() == null) {
			return ResponseEntity.status(401).body("Not authenticated");
		}

		User citizen = userRepository.findByEmail(auth.getName());
		if (citizen == null) {
			return ResponseEntity.status(404).body("User not found");
		}

		// ✅ Convert department label to enum using DEPT_MAP
		String deptLabel = body.get("department").toString();
		Department deptEnum = DEPT_MAP.get(deptLabel);
		if (deptEnum == null) {
			System.out.println("❌ Unknown department: " + deptLabel);
			return ResponseEntity.status(400).body("Unknown department: " + deptLabel);
		}
		System.out.println("✅ Department: " + deptLabel + " → " + deptEnum);

		Complaint complaint = new Complaint();
		complaint.setTitle(body.get("title").toString());
		complaint.setDescription(body.get("description").toString());
		complaint.setLocation(body.getOrDefault("location", "").toString());
		complaint.setDepartment(deptEnum);
		complaint.setPriority(ComplaintPriority.valueOf(body.getOrDefault("priority", "LOW").toString()));
		complaint.setUser(citizen);

		// Coordinates
		try {
			Object lat = body.get("latitude");
			Object lng = body.get("longitude");
			if (lat != null && lng != null && !lat.toString().equals("null") && !lng.toString().equals("null")) {
				complaint.setLatitude(Double.parseDouble(lat.toString()));
				complaint.setLongitude(Double.parseDouble(lng.toString()));
			}
		} catch (NumberFormatException ignored) {
		}

		// ✅ Auto-assign officer
		List<User> officers = userRepository.findOfficersByDepartmentOrderByLoad(
			    Role.OFFICER, complaint.getDepartment()
			);
			User officer = officers.isEmpty() ? null : officers.get(0);

		complaint.getDepartments().add(deptEnum);

		if (officer != null) {
			complaint.setAssignedOfficer(officer);
			complaint.setStatus(ComplaintStatus.IN_PROGRESS);
			System.out.println("✅ Officer assigned: " + officer.getName());

			// Notify citizen
			Notification citizenNotif = new Notification();
			citizenNotif.setMessage("Your complaint '" + complaint.getTitle() + "' has been assigned to officer "
					+ officer.getName() + " from " + deptEnum.name() + " department.");
			citizenNotif.setRole("CITIZEN");
			citizenNotif.setUser(citizen);
			citizenNotif.setCreatedAt(java.time.LocalDateTime.now());
			notificationRepository.save(citizenNotif);

			// Notify officer
			Notification officerNotif = new Notification();
			officerNotif.setMessage(
					"New complaint assigned: '" + complaint.getTitle() + "' — Priority: " + complaint.getPriority());
			officerNotif.setRole("OFFICER");
			officerNotif.setUser(officer);
			officerNotif.setCreatedAt(java.time.LocalDateTime.now());
			notificationRepository.save(officerNotif);

		} else {
			complaint.setStatus(ComplaintStatus.OPEN);
			System.out.println("⚠️ No officer found for: " + deptEnum);
		}

		Complaint saved = complaintRepository.save(complaint);
		System.out.println("✅ Complaint saved with ID: " + saved.getId());

		return ResponseEntity.ok(
				Map.of("id", saved.getId(), "title", saved.getTitle(), "status", saved.getStatus().name(), "department",
						saved.getDepartment().name(), "officer", officer != null ? officer.getName() : "Unassigned"));
	}
}