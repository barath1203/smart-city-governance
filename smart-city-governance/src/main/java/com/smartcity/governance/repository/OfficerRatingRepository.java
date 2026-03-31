package com.smartcity.governance.repository;

import com.smartcity.governance.model.OfficerRating;
import com.smartcity.governance.model.RatingSource;
import com.smartcity.governance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OfficerRatingRepository extends JpaRepository<OfficerRating, Long> {

    List<OfficerRating> findByOfficer(User officer);

    List<OfficerRating> findByOfficerAndSource(User officer, RatingSource source);

    // Prevent duplicate rating — one per complaint per source
    Optional<OfficerRating> findByComplaintIdAndSource(Long complaintId, RatingSource source);

    // For performance recalculation
    List<OfficerRating> findByOfficerId(Long officerId);
}