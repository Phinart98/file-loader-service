package com.assessment.fileloader.repository;

import com.assessment.fileloader.model.CdrLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdrLogRepository extends JpaRepository<CdrLog, Long> {

    /**
     * Check if a file has already been processed
     * @param fileName the name of the file to check
     * @return true if the file has been processed, false otherwise
     */
    boolean existsByFileName(String fileName);
}
