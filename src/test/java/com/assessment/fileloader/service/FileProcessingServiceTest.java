package com.assessment.fileloader.service;

import com.assessment.fileloader.model.CallDetailRecord;
import com.assessment.fileloader.model.CdrLog;
import com.assessment.fileloader.repository.CallDetailRecordRepository;
import com.assessment.fileloader.repository.CdrLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private CallDetailRecordRepository callDetailRecordRepository;

    @Mock
    private CdrLogRepository cdrLogRepository;

    @InjectMocks
    private FileProcessingService fileProcessingService;

    @Captor
    private ArgumentCaptor<List<CallDetailRecord>> recordsCaptor;

    @Captor
    private ArgumentCaptor<CdrLog> cdrLogCaptor;

    private File testFile;
    private String processedDirectory;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary test file with sample CDR data
        testFile = File.createTempFile("test-cdr-", ".log");
        testFile.deleteOnExit();

        // Write sample data to the test file
        String sampleData = "2023-08-18 10:00:00,024|15845|15|0|4|573103154359||6|0|4|573103804442|*611#|1|1|573103154393|1|6|732101647793504|1|1|573228553366|||||FAILED_DIALOG_USER_ABORT|PULL|2023-08-18 10:00:00.024|5948547|924990671|50141|3,2,2,1,1|1c3394ad-2ac0-4bcb-9d87-882a442ea947\n" +
                "2023-08-18 10:00:00,025|15845|15|0|4|573103154359||6|0|4|573103804442|*611#|1|1|573103154393|1|6|732101647793504|1|1|573228553367|||||FAILED_DIALOG_USER_ABORT|PULL|2023-08-18 10:00:00.025|5948548|924990672|50142|3,2,2,1,1|2c3394ad-2ac0-4bcb-9d87-882a442ea948";
        Files.writeString(testFile.toPath(), sampleData);

        // Create a temporary processed directory
        processedDirectory = Files.createTempDirectory("processed-").toString();
    }

    @Test
    void hasBeenProcessed_shouldCheckRepository() {
        // Given
        String fileName = "test.log";
        when(cdrLogRepository.existsByFileName(fileName)).thenReturn(true);

        // When
        boolean result = fileProcessingService.hasBeenProcessed(fileName);

        // Then
        assertTrue(result);
        verify(cdrLogRepository).existsByFileName(fileName);
    }

    @Test
    void processFile_shouldParseRecordsAndSaveToDatabase() throws IOException {
        // Given
        when(cdrLogRepository.save(any(CdrLog.class))).thenReturn(new CdrLog());

        // When
        fileProcessingService.processFile(testFile, processedDirectory);

        // Then
        verify(callDetailRecordRepository).saveAll(recordsCaptor.capture());
        verify(cdrLogRepository).save(cdrLogCaptor.capture());

        List<CallDetailRecord> capturedRecords = recordsCaptor.getValue();
        CdrLog capturedLog = cdrLogCaptor.getValue();

        // Verify records were parsed correctly
        assertEquals(2, capturedRecords.size());
        assertEquals("573228553366", capturedRecords.get(0).getMsisdn());
        assertEquals("573228553367", capturedRecords.get(1).getMsisdn());

        // Verify log entry was created correctly
        assertEquals(testFile.getName(), capturedLog.getFileName());
        assertEquals(2, capturedLog.getSuccessCount());
        assertEquals(0, capturedLog.getFailedCount());
        assertNotNull(capturedLog.getUploadStartTime());
        assertNotNull(capturedLog.getUploadEndTime());
    }

    @Test
    void processFile_shouldHandleParsingErrors() throws IOException {
        // Given
        String invalidData = "2023-08-18 10:00:00,024|invalid|data|that|will|cause|parsing|errors\n" +
                "2023-08-18 10:00:00,025|15845|15|0|4|573103154359||6|0|4|573103804442|*611#|1|1|573103154393|1|6|732101647793504|1|1|573228553367|||||FAILED_DIALOG_USER_ABORT|PULL|2023-08-18 10:00:00.025|5948548|924990672|50142|3,2,2,1,1|2c3394ad-2ac0-4bcb-9d87-882a442ea948";
        Files.writeString(testFile.toPath(), invalidData);

        when(cdrLogRepository.save(any(CdrLog.class))).thenReturn(new CdrLog());

        // When
        fileProcessingService.processFile(testFile, processedDirectory);

        // Then
        verify(callDetailRecordRepository).saveAll(recordsCaptor.capture());
        verify(cdrLogRepository).save(cdrLogCaptor.capture());

        List<CallDetailRecord> capturedRecords = recordsCaptor.getValue();
        CdrLog capturedLog = cdrLogCaptor.getValue();

        // Verify only valid records were parsed
        assertEquals(1, capturedRecords.size());

        // Verify log entry counts errors correctly
        assertEquals(testFile.getName(), capturedLog.getFileName());
        assertEquals(1, capturedLog.getSuccessCount());
        assertEquals(1, capturedLog.getFailedCount());
    }

    @Test
    void processFile_shouldHandleIOException() throws IOException {
        // Given
        File nonExistentFile = new File("non-existent-file.log");

        // When
        fileProcessingService.processFile(nonExistentFile, processedDirectory);

        // Then
        verify(callDetailRecordRepository, never()).saveAll(any());
        verify(cdrLogRepository).save(cdrLogCaptor.capture());

        CdrLog capturedLog = cdrLogCaptor.getValue();
        assertEquals(nonExistentFile.getName(), capturedLog.getFileName());
        assertEquals(0, capturedLog.getSuccessCount());
        assertEquals(0, capturedLog.getFailedCount());
    }
}