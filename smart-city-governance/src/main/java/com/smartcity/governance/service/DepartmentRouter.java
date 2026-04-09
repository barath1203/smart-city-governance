package com.smartcity.governance.service;

import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.Department;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepartmentRouter {

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

    private static final List<KeywordEntry> TAMIL_KEYWORD_TABLE = List.of(
        new KeywordEntry("கசிவு",          Department.WATER,       3),
        new KeywordEntry("குழாய்",          Department.WATER,       2),
        new KeywordEntry("தண்ணீர்",         Department.WATER,       1),
        new KeywordEntry("நீர்",            Department.WATER,       1),
        new KeywordEntry("வெள்ளம்",         Department.WATER,       2),
        new KeywordEntry("வடிகால்",         Department.WATER,       2),
        new KeywordEntry("கழிவுநீர்",       Department.WATER,       3),
        new KeywordEntry("சாக்கடை",         Department.WATER,       3),
        new KeywordEntry("தண்ணி",           Department.WATER,       1),
        new KeywordEntry("பைப்",            Department.WATER,       2),
        new KeywordEntry("குழி",            Department.ROAD,        3),
        new KeywordEntry("பள்ளம்",          Department.ROAD,        3),
        new KeywordEntry("நடைபாதை",         Department.ROAD,        2),
        new KeywordEntry("சாலை",            Department.ROAD,        1),
        new KeywordEntry("தார்சாலை",        Department.ROAD,        2),
        new KeywordEntry("விபத்து",          Department.ROAD,        2),
        new KeywordEntry("ரோடு",            Department.ROAD,        1),
        new KeywordEntry("குப்பை",          Department.SANITATION,  3),
        new KeywordEntry("கழிவு",           Department.SANITATION,  2),
        new KeywordEntry("துப்புரவு",        Department.SANITATION,  2),
        new KeywordEntry("அழுக்கு",         Department.SANITATION,  2),
        new KeywordEntry("நாற்றம்",         Department.SANITATION,  1),
        new KeywordEntry("தூய்மை",          Department.SANITATION,  1),
        new KeywordEntry("மின்சாரம்",       Department.ELECTRICITY, 3),
        new KeywordEntry("மின்வெட்டு",      Department.ELECTRICITY, 3),
        new KeywordEntry("மின்",            Department.ELECTRICITY, 1),
        new KeywordEntry("விளக்கு",         Department.ELECTRICITY, 1),
        new KeywordEntry("தெருவிளக்கு",     Department.ELECTRICITY, 3),
        new KeywordEntry("கம்பி",           Department.ELECTRICITY, 2),
        new KeywordEntry("ட்ரான்ஸ்பார்மர்", Department.ELECTRICITY, 2)
    );

    private static final Set<String> LOCATION_PREPOSITIONS = Set.of(
        "near", "beside", "opposite", "behind", "next", "around",
        "அருகில்", "அருகே", "பக்கத்தில்", "எதிரில்", "பின்னால்", "அருகு"
    );

    private static final Map<String, Integer> SENSITIVE_LOCATIONS = new LinkedHashMap<>() {{
        put("hospital",   2);  put("school",     1);  put("college",    1);
        put("bus stand",  1);  put("busstand",   1);  put("market",     1);
        put("temple",     1);  put("mosque",     1);  put("church",     1);
        put("playground", 1);
        put("மருத்துவமனை",        2);
        put("பள்ளி",               1);
        put("கல்லூரி",             1);
        put("பேருந்து நிலையம்",   1);
        put("சந்தை",               1);
        put("கோயில்",              1);
        put("மசூதி",               1);
        put("திருச்சபை",           1);
        put("விளையாட்டு மைதானம்",  1);
    }};

    public CategorizationResult categorize(String input) {
        boolean isTamil = containsTamil(input);

        String cleaned = isTamil
            ? input.trim().replaceAll("\\s+", " ")
            : preprocess(input);

        String[] words = cleaned.split("\\s+");

        // Collect ALL dept keywords so extractLocationContext never blocks them
        Set<String> allDeptKeywords = new HashSet<>();
        for (KeywordEntry e : KEYWORD_TABLE)       allDeptKeywords.add(e.keyword);
        for (KeywordEntry e : TAMIL_KEYWORD_TABLE)  allDeptKeywords.add(e.keyword);

        Set<String> locationContextWords = extractLocationContext(words, allDeptKeywords);

        Map<Department, Integer> scores = new EnumMap<>(Department.class);
        for (Department d : Department.values()) scores.put(d, 0);

        List<KeywordEntry> table = isTamil ? TAMIL_KEYWORD_TABLE : KEYWORD_TABLE;
        for (KeywordEntry entry : table) {
            if (locationContextWords.contains(entry.keyword)) continue;
            if (cleaned.contains(entry.keyword)) {
                scores.merge(entry.department, entry.weight, Integer::sum);
            }
        }

        // Mixed-input fallback (e.g. "road குழி")
        if (isTamil && scores.values().stream().allMatch(s -> s == 0)) {
            String englishCleaned = preprocess(input);
            for (KeywordEntry entry : KEYWORD_TABLE) {
                if (englishCleaned.contains(entry.keyword)) {
                    scores.merge(entry.department, entry.weight, Integer::sum);
                }
            }
        }

        Department winner = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .orElse(Department.ROAD);

        int topScore   = scores.get(winner);
        int totalScore = scores.values().stream().mapToInt(i -> i).sum();
        int confidence = totalScore > 0
            ? (int) Math.round((double) topScore / totalScore * 100) : 0;

        boolean conflict = scores.values().stream()
            .filter(s -> s.equals(topScore) && topScore > 0).count() > 1;

        int priorityBoost      = 0;
        String detectedLocation = null;
        for (Map.Entry<String, Integer> loc : SENSITIVE_LOCATIONS.entrySet()) {
            if (cleaned.contains(loc.getKey())) {
                if (loc.getValue() > priorityBoost) {
                    priorityBoost    = loc.getValue();
                    detectedLocation = loc.getKey();
                }
            }
        }

        ComplaintPriority priority = switch (priorityBoost) {
            case 2  -> ComplaintPriority.EMERGENCY;
            case 1  -> ComplaintPriority.HIGH;
            default -> ComplaintPriority.LOW;
        };

        return new CategorizationResult(winner, priority, confidence, conflict, detectedLocation);
    }

    /**
     * Collects words after a location preposition as "location context",
     * but NEVER blocks a word that is itself a department keyword.
     *
     * Example: "மருத்துவமனை அருகில் குழாய் கசிவு"
     *   preposition = அருகில்
     *   next        = குழாய்  → IS a dept keyword → NOT blocked ✅ → scores WATER
     *   next+1      = கசிவு   → IS a dept keyword → NOT blocked ✅ → scores WATER
     *
     * Example: "water leakage near school"
     *   preposition = near
     *   next        = school  → NOT a dept keyword → blocked ✅ (avoids ROAD misroute)
     */
    private Set<String> extractLocationContext(String[] words, Set<String> allDeptKeywords) {
        Set<String> contextWords = new HashSet<>();
        for (int i = 0; i < words.length - 1; i++) {
            if (LOCATION_PREPOSITIONS.contains(words[i])) {
                String next = words[i + 1];
                if (!allDeptKeywords.contains(next)) contextWords.add(next);
                if (i + 2 < words.length) {
                    String next2 = words[i + 2];
                    if (!allDeptKeywords.contains(next2)) contextWords.add(next2);
                }
            }
        }
        return contextWords;
    }

    private boolean containsTamil(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x0B80 && c <= 0x0BFF) return true;
        }
        return false;
    }

    private String preprocess(String input) {
        return input.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private record KeywordEntry(String keyword, Department department, int weight) {}

    public record CategorizationResult(
        Department department,
        ComplaintPriority priority,
        int confidence,
        boolean conflict,
        String sensitiveLocation
    ) {}
}