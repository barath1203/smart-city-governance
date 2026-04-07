package com.smartcity.governance.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.CoordinationAssignment;
import com.smartcity.governance.model.User;

public interface CoordinationAssignmentRepository
        extends JpaRepository<CoordinationAssignment, Long> {

    List<CoordinationAssignment> findByComplaint(Complaint complaint);

    List<CoordinationAssignment> findByOfficerId(Long officerId);

    List<CoordinationAssignment> findByComplaintAndActiveTrue(Complaint complaint); // 🔥 useful
    
    List<CoordinationAssignment> findByOfficerAndActiveTrue(User officer);
}
