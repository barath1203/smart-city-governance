package com.smartcity.governance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.UserRepository;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔹 1. Raise a new complaint
    @PostMapping("/create/{userId}")
    public Complaint createComplaint(
            @PathVariable Long userId,
            @RequestBody Complaint complaint) {

        User user = userRepository.findById(userId).orElseThrow();
        complaint.setUser(user);

        return complaintRepository.save(complaint);
    }

    // 🔹 2. Get all complaints (Admin / Officer)
    @GetMapping("/all")
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // 🔹 3. Get complaints by user (Citizen)
    @GetMapping("/user/{userId}")
    public List<Complaint> getComplaintsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return complaintRepository.findByUser(user);
    }

    // 🔹 4. Update complaint status (Officer/Admin)
    @PutMapping("/update-status/{complaintId}")
    public Complaint updateStatus(
            @PathVariable Long complaintId,
            @RequestParam String status) {

        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        complaint.setStatus(status);

        return complaintRepository.save(complaint);
    }
}
