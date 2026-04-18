package com.smartcity.governance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.Role;
import com.smartcity.governance.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	@Query("SELECT u FROM User u WHERE u.role = :role AND u.department = :department " +
		       "ORDER BY (SELECT COUNT(c) FROM Complaint c WHERE c.assignedOfficer = u " +
		       "AND c.status != 'RESOLVED') ASC")
		List<User> findOfficersByDepartmentOrderByLoad(
		    @Param("role") Role role,
		    @Param("department") Department department
		);

    User findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentAndRole(Department department, Role role);

    User findFirstByRoleAndDepartment(Role role, Department department);

    User findFirstByRole(Role role);
}