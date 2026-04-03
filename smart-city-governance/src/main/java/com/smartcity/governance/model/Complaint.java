package com.smartcity.governance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "complaints")
public class Complaint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;
	private String description;
	private String location;
	private Double latitude;
	private Double longitude;
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	private ComplaintPriority priority;

	@Enumerated(EnumType.STRING)
	private ComplaintStatus status;

	@ElementCollection(targetClass = Department.class)
	@Enumerated(EnumType.STRING)
	@CollectionTable(name = "complaint_departments",
	        joinColumns = @JoinColumn(name = "complaint_id"))
	@Column(name = "department")
	private List<Department> departments = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	private Department department;


	private LocalDateTime createdAt;
	private LocalDateTime deadline;
	@Column
	private LocalDateTime resolvedAt;
	private boolean escalated = false;

	// ✅ Rating fields
	private Integer rating;
	private String ratingComment;
	private boolean rated = false;

	@ManyToOne
	@JoinColumn(name = "citizen_id")
	private User user;

	@ManyToOne
	@JoinColumn(name = "officer_id")
	private User assignedOfficer;

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.status = ComplaintStatus.OPEN;
		this.deadline = calculateDeadline(this.priority);
	}

	private LocalDateTime calculateDeadline(ComplaintPriority priority) {
		if (priority == null) {
			return LocalDateTime.now().plusDays(5);
		}
		return switch (priority) {
		case LOW -> LocalDateTime.now().plusMinutes(1);
		case MEDIUM -> LocalDateTime.now().plusMinutes(1);
		case HIGH -> LocalDateTime.now().plusMinutes(1);
		case EMERGENCY -> LocalDateTime.now().plusMinutes(1);
		};
	}

	// Getters and Setters
	public Long getId() {
		return id;
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

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
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

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public ComplaintPriority getPriority() {
		return priority;
	}

	public void setPriority(ComplaintPriority priority) {
		this.priority = priority;
	}

	public ComplaintStatus getStatus() {
		return status;
	}

	public void setStatus(ComplaintStatus status) {
		this.status = status;
	}

	public List<Department> getDepartments() {
	    return departments;
	}

	public void setDepartments(List<Department> departments) {
	    this.departments = departments;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getDeadline() {
		return deadline;
	}

	public void setDeadline(LocalDateTime deadline) {
		this.deadline = deadline;
	}

	public boolean isEscalated() {
		return escalated;
	}

	public void setEscalated(boolean escalated) {
		this.escalated = escalated;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getAssignedOfficer() {
		return assignedOfficer;
	}

	public void setAssignedOfficer(User assignedOfficer) {
		this.assignedOfficer = assignedOfficer;
	}

	// ✅ Rating getters and setters
	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getRatingComment() {
		return ratingComment;
	}

	public void setRatingComment(String ratingComment) {
		this.ratingComment = ratingComment;
	}

	public boolean isRated() {
		return rated;
	}

	public void setRated(boolean rated) {
		this.rated = rated;
	}

	public LocalDateTime getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(LocalDateTime resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}