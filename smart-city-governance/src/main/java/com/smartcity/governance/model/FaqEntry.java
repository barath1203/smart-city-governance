package com.smartcity.governance.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "faq_entries")
public class FaqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "faq_keywords",
        joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "keyword")
    private List<String> keywords;

    @Column(columnDefinition = "TEXT")
    private String answer;

    public Long getId() { return id; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}