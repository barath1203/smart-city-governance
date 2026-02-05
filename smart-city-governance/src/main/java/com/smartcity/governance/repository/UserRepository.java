package com.smartcity.governance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcity.governance.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    //Optional<User> findByUsername(String name);
    //Optional<User> findByEmail(String email);
}
