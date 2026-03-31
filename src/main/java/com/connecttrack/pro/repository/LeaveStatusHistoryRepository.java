package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.LeaveStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveStatusHistoryRepository extends JpaRepository<LeaveStatusHistory, Long> {
    // This repository is mainly for saving, so it doesn't need custom query methods yet.
}