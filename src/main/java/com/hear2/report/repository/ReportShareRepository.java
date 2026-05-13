package com.hear2.report.repository;

import com.hear2.report.entity.ReportShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportShareRepository extends JpaRepository<ReportShare, Long> {

    Optional<ReportShare> findByShareCode(String shareCode);
}
