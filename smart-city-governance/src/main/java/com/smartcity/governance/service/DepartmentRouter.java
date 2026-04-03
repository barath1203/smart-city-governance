package com.smartcity.governance.service;

import com.smartcity.governance.model.Department;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class DepartmentRouter {

    private static final Map<String, Department> KEYWORD_MAP = Map.ofEntries(
        Map.entry("water",       Department.WATER),
        Map.entry("pipe",        Department.WATER),
        Map.entry("leak",        Department.WATER),
        Map.entry("flood",       Department.WATER),
        Map.entry("electricity", Department.ELECTRICITY),
        Map.entry("light",       Department.ELECTRICITY),
        Map.entry("power",       Department.ELECTRICITY),
        Map.entry("road",        Department.ROAD),
        Map.entry("pothole",     Department.ROAD),
        Map.entry("footpath",    Department.ROAD),
        Map.entry("garbage",     Department.SANITATION),
        Map.entry("waste",       Department.SANITATION),
        Map.entry("sanitation",  Department.SANITATION)
    );

    public Department route(String text) {
        String lower = text.toLowerCase();
        return KEYWORD_MAP.entrySet().stream()
            .filter(e -> lower.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(Department.ROAD);
    }
}