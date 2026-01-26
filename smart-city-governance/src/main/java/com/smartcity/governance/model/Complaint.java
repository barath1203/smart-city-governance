package com.smartcity.governance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private Double latitude;
    private Double longitude;

    private String status; 
    // OPEN, IN_PROGRESS, RESOLVED

    private LocalDateTime createdAt;

    // 🔗 Many complaints → One user (Citizen)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Constructors
    public Complaint() {
        this.createdAt = LocalDateTime.now();
        this.status = "OPEN";
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
 
    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }
 
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
 
    public Double getLongitude() {
        return longitude;
    }
 
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
 
    public String getStatus() {
        return status;
    }
 
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }
 
    public void setUser(User user) {
        this.user = user;
    }
}
