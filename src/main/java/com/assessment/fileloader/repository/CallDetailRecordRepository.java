package com.assessment.fileloader.repository;

import com.assessment.fileloader.model.CallDetailRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallDetailRecordRepository extends JpaRepository<CallDetailRecord, Long> {
}
