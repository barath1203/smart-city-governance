package com.smartcity.governance.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcity.governance.model.Complaint;
import com.smartcity.governance.model.ComplaintStatus;
import com.smartcity.governance.model.OfficerRating;
import com.smartcity.governance.model.RatingSource;
import com.smartcity.governance.model.User;
import com.smartcity.governance.repository.ComplaintRepository;
import com.smartcity.governance.repository.OfficerRatingRepository;
import com.smartcity.governance.repository.UserRepository;

@Service
public class PerformanceService {

    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private OfficerRatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;

    public void recalculateScore(User officer) {
        List<Complaint> assigned = complaintRepository.findByAssignedOfficer(officer);

        int total     = assigned.size();
        int resolved  = (int) assigned.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        int escalated = (int) assigned.stream()
            .filter(c -> c.isEscalated()).count();

        double resolutionRate    = total > 0 ? (resolved * 100.0 / total) : 0;
        double escalationPenalty = total > 0 ? (escalated * 100.0 / total) : 0;

        long onTimeResolved = assigned.stream()
        	    .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED &&
        	                 c.getResolvedAt() != null &&
        	                 c.getDeadline() != null &&
        	                 c.getResolvedAt().isBefore(c.getDeadline()))
        	    .count();

        	double slaScore = total > 0 ? (onTimeResolved * 100.0 / total) : 0;

        // Pull ratings from OfficerRating table
        List<OfficerRating> dhRatings = ratingRepository
            .findByOfficerAndSource(officer, RatingSource.DEPARTMENT_HEAD);
        List<OfficerRating> citizenRatings = ratingRepository
            .findByOfficerAndSource(officer, RatingSource.CITIZEN);

        double dhAvg = dhRatings.stream()
            .mapToInt(OfficerRating::getRating)
            .average().orElse(0.0);

        double citizenAvg = citizenRatings.stream()
            .mapToInt(OfficerRating::getRating)
            .average().orElse(0.0);

        int score = (int) (
        	    (resolutionRate * 0.25) +
        	    ((100 - escalationPenalty) * 0.20) +
        	    (slaScore * 0.20) +
        	    (dhAvg / 5.0 * 100 * 0.20) +
        	    (citizenAvg / 5.0 * 100 * 0.15)
        	);

        for (OfficerRating r : dhRatings) {
            if (r.getFeedback() != null) {
                String f = r.getFeedback().toLowerCase();
                if (f.contains("delay")) {
					score -= 5;
				}
                if (f.contains("excellent")) {
					score += 5;
				}
            }
        }

        officer.setPerformanceScore(Math.min(score, 100));
        userRepository.save(officer);
    }
}
