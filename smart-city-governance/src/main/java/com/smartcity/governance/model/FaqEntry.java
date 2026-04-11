package com.smartcity.governance.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "faq_entries")
public class FaqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", length = 500)   // ← ADD THIS
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @ElementCollection(fetch = FetchType.EAGER)  // ← ADD EAGER (prevents lazy load issues)
    @CollectionTable(
        name = "faq_keywords",
        joinColumns = @JoinColumn(name = "faq_id")
    )
    @Column(name = "keyword")
    private List<String> keywords;

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }

    public String getQuestion()              { return question; }        // ← ADD
    public void setQuestion(String question) { this.question = question; } // ← ADD

    public String getAnswer()                { return answer; }
    public void setAnswer(String answer)     { this.answer = answer; }

    public List<String> getKeywords()                  { return keywords; }
    public void setKeywords(List<String> keywords)     { this.keywords = keywords; }
}