package com.smartcity.governance.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintPriority;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.User;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    long countByStatus(ComplaintStatus status);
    long countByDepartment(Department department);

    @Query("SELECT c FROM Complaint c WHERE c.user = :user")
    List<Complaint> findByUser(User user);

    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findByDepartment(Department department);
    List<Complaint> findByAssignedOfficer(User officer);
    List<Complaint> findByAssignedOfficerAndStatus(User officer, ComplaintStatus status);
    List<Complaint> findByAssignedOfficerAndPriority(User officer, ComplaintPriority priority);

    // ✅ NEW — returns all complaints sorted by priority (EMERGENCY first)
    List<Complaint> findAllByOrderByPriorityDesc();
}