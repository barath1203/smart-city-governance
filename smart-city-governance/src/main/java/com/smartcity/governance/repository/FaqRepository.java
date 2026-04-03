package com.smartcity.governance.repository;

import com.smartcity.governance.model.FaqEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FaqRepository extends JpaRepository<FaqEntry, Long> {
    List<FaqEntry> findAll();
}