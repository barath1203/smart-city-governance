	package com.smartcity.governance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcity.governance.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
