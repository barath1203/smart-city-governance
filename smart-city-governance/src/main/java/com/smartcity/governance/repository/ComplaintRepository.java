package com.smartcity.governance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.User;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
	
	long countByStatus(String status);

	long countByDepartment(String department);

    List<Complaint> findByUser(User user);

    List<Complaint> findByStatus(String status);
    
    List<Complaint> findByDepartment(String department);

    List<Complaint> findByAssignedOfficer(User officer);

}
