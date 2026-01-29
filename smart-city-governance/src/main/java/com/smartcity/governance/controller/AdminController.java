package com.smartcity.governance.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.smartcity.governance.repository.ComplaintRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ComplaintRepository complaintRepository;

    // 🔹 Dashboard Analytics
    @GetMapping("/dashboard")
    public Map<String, Long> dashboardStats() {

        Map<String, Long> stats = new HashMap<>();

        stats.put("TOTAL_COMPLAINTS", complaintRepository.count());
        stats.put("OPEN", complaintRepository.countByStatus("OPEN"));
        stats.put("IN_PROGRESS", complaintRepository.countByStatus("IN_PROGRESS"));
        stats.put("RESOLVED", complaintRepository.countByStatus("RESOLVED"));

        stats.put("WATER", complaintRepository.countByDepartment("WATER"));
        stats.put("ELECTRICITY", complaintRepository.countByDepartment("ELECTRICITY"));
        stats.put("ROAD", complaintRepository.countByDepartment("ROAD"));
        stats.put("SANITATION", complaintRepository.countByDepartment("SANITATION"));

        return stats;
    }
}
