package com.assessment.fileloader.repository;

import com.assessment.fileloader.model.CdrLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CdrLogRepository extends JpaRepository<CdrLog, Long> {
    Optional<CdrLog> findByFileName(String fileName);
}
