package com.smartcity.governance.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.User;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUser(User user);
    List<Complaint> findByAssignedOfficer(User assignedOfficer);
    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findByAssignedOfficerAndStatus(User officer, ComplaintStatus status);
    List<Complaint> findByAssignedOfficerAndPriority(User officer, ComplaintPriority priority);
    List<Complaint> findAllByOrderByPriorityDesc();

    long countByStatus(ComplaintStatus status);
    long countByDepartment(Department department);
    long countByPriority(ComplaintPriority priority);

    @Query("SELECT c FROM Complaint c WHERE c.status = 'OPEN' AND c.deadline < :now AND c.escalated = false")
    List<Complaint> findOverdueComplaints(@Param("now") LocalDateTime now);

    List<Complaint> findByEscalatedTrue();
    List<Complaint> findByRatedTrue();
    List<Complaint> findByAssignedOfficerAndRatedTrue(User officer);
    List<Complaint> findByDepartment(Department department);
    List<Complaint> findByDepartmentAndEscalatedTrue(Department department);

    // ✅ Added for chatbot — fetch active complaints of logged-in citizen
    List<Complaint> findByUserAndStatusNot(User user, ComplaintStatus status);

    // ✅ Added for chatbot — fetch complaint only if it belongs to logged-in citizen
    Optional<Complaint> findByIdAndUser(Long id, User user);
}