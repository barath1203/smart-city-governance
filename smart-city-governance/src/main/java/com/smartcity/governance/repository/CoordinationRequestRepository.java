package com.smartcity.governance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcity.governance.model.CoordinationRequest;
import com.smartcity.governance.model.RequestStatus;

public interface CoordinationRequestRepository 
extends JpaRepository<CoordinationRequest, Long> {

List<CoordinationRequest> findByStatus(RequestStatus status);
}