package com.smartcity.governance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.smartcity.governance.model.Department;
import com.smartcity.governance.model.Role;
import com.smartcity.governance.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentAndRole(Department department, Role role);
    
    User findFirstByRoleAndDepartment(Role role, Department department);
}