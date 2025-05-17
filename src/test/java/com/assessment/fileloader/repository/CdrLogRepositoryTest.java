package com.assessment.fileloader.repository;

import com.assessment.fileloader.model.CdrLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CdrLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CdrLogRepository cdrLogRepository;

    @Test
    void existsByFileName_shouldReturnTrueWhenFileExists() {
        // Given
        String fileName = "test-file.log";
        CdrLog cdrLog = new CdrLog();
        cdrLog.setFileName(fileName);
        cdrLog.setUploadStartTime(LocalDateTime.now());
        cdrLog.setUploadEndTime(LocalDateTime.now());
        cdrLog.setSuccessCount(10);
        cdrLog.setFailedCount(0);

        entityManager.persist(cdrLog);
        entityManager.flush();

        // When
        boolean exists = cdrLogRepository.existsByFileName(fileName);

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByFileName_shouldReturnFalseWhenFileDoesNotExist() {
        // When
        boolean exists = cdrLogRepository.existsByFileName("non-existent-file.log");

        // Then
        assertFalse(exists);
    }
}