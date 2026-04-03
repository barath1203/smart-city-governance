package com.smartcity.governance.service;

import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.Department;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepartmentRouter {

    // Keyword → (Department, weight)
    private static final List<KeywordEntry> KEYWORD_TABLE = List.of(
        new KeywordEntry("leakage",     Department.WATER,       3),
        new KeywordEntry("pipe",        Department.WATER,       2),
        new KeywordEntry("water",       Department.WATER,       1),
        new KeywordEntry("flood",       Department.WATER,       2),
        new KeywordEntry("drain",       Department.WATER,       2),
        new KeywordEntry("sewage",      Department.WATER,       3),
        new KeywordEntry("pothole",     Department.ROAD,        3),
        new KeywordEntry("footpath",    Department.ROAD,        2),
        new KeywordEntry("road",        Department.ROAD,        1),
        new KeywordEntry("pavement",    Department.ROAD,        2),
        new KeywordEntry("accident",    Department.ROAD,        2),
        new KeywordEntry("garbage",     Department.SANITATION,  3),
        new KeywordEntry("waste",       Department.SANITATION,  2),
        new KeywordEntry("sanitation",  Department.SANITATION,  2),
        new KeywordEntry("dustbin",     Department.SANITATION,  3),
        new KeywordEntry("smell",       Department.SANITATION,  1),
        new KeywordEntry("electricity", Department.ELECTRICITY, 3),
        new KeywordEntry("power",       Department.ELECTRICITY, 2),
        new KeywordEntry("light",       Department.ELECTRICITY, 1),
        new KeywordEntry("outage",      Department.ELECTRICITY, 3),
        new KeywordEntry("wire",        Department.ELECTRICITY, 2),
        new KeywordEntry("streetlight", Department.ELECTRICITY, 3)
    );

    // Location context words — words after these are NOT category keywords
    private static final Set<String> LOCATION_PREPOSITIONS = Set.of(
        "near", "beside", "opposite", "behind", "next", "around"
    );

    // Sensitive locations and their priority boost scores
    private static final Map<String, Integer> SENSITIVE_LOCATIONS = Map.of(
        "hospital",    2,
        "school",      1,
        "college",     1,
        "busstand",    1,
        "bus stand",   1,
        "market",      1,
        "temple",      1,
        "mosque",      1,
        "church",      1,
        "playground",  1
    );

    public CategorizationResult categorize(String input) {
        String cleaned   = preprocess(input);
        String[] words   = cleaned.split("\\s+");
        Set<String> locationContextWords = extractLocationContext(words);

        // Score each department
        Map<Department, Integer> scores = new EnumMap<>(Department.class);
        for (Department d : Department.values()) scores.put(d, 0);

        for (KeywordEntry entry : KEYWORD_TABLE) {
            // Skip if this keyword appears after a location preposition
            if (locationContextWords.contains(entry.keyword)) continue;
            if (cleaned.contains(entry.keyword)) {
                scores.merge(entry.department, entry.weight, Integer::sum);
            }
        }

        // Find winning department
        Department winner = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .orElse(Department.ROAD);

        int topScore  = scores.get(winner);
        int totalScore = scores.values().stream().mapToInt(i -> i).sum();
        int confidence = totalScore > 0
            ? (int) Math.round((double) topScore / totalScore * 100) : 0;

        // Check for conflicts — two depts with same score
        boolean conflict = scores.values().stream()
            .filter(s -> s.equals(topScore) && topScore > 0).count() > 1;

        // Sensitive location detection
        int priorityBoost = 0;
        String detectedLocation = null;
        for (Map.Entry<String, Integer> loc : SENSITIVE_LOCATIONS.entrySet()) {
            if (cleaned.contains(loc.getKey())) {
                if (loc.getValue() > priorityBoost) {
                    priorityBoost     = loc.getValue();
                    detectedLocation  = loc.getKey();
                }
            }
        }

        // Priority calculation
        ComplaintPriority priority = switch (priorityBoost) {
            case 2  -> ComplaintPriority.EMERGENCY;
            case 1  -> ComplaintPriority.HIGH;
            default -> ComplaintPriority.LOW;
        };

        return new CategorizationResult(
            winner, priority, confidence,
            conflict, detectedLocation
        );
    }

    private String preprocess(String input) {
        return input.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private Set<String> extractLocationContext(String[] words) {
        Set<String> contextWords = new HashSet<>();
        for (int i = 0; i < words.length - 1; i++) {
            if (LOCATION_PREPOSITIONS.contains(words[i])) {
                contextWords.add(words[i + 1]);
                if (i + 2 < words.length) contextWords.add(words[i + 2]);
            }
        }
        return contextWords;
    }

    // Inner classes
    private record KeywordEntry(String keyword, Department department, int weight) {}

    public record CategorizationResult(
        Department department,
        ComplaintPriority priority,
        int confidence,
        boolean conflict,
        String sensitiveLocation
    ) {}
}