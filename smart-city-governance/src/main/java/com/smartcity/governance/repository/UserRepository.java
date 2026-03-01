package com.smartcity.governance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.smartcity.governance.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    List<User> findByRole(String role);

    User findFirstByRoleAndDepartment(String role, String department);
}