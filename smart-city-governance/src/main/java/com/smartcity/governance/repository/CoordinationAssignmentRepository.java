package com.smartcity.governance.repository;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcity.governance.model.CoordinationAssignment;
import com.smartcity.governance.model.Complaint;

public interface CoordinationAssignmentRepository 
        extends JpaRepository<CoordinationAssignment, Long> {

    List<CoordinationAssignment> findByComplaint(Complaint complaint);

    List<CoordinationAssignment> findByOfficerId(Long officerId);

    List<CoordinationAssignment> findByComplaintAndActiveTrue(Complaint complaint); // 🔥 useful
}
