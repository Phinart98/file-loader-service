package com.assessment.fileloader.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMonitoringServiceTest {

    @Mock
    private FileProcessingService fileProcessingService;

    @InjectMocks
    private FileMonitoringService fileMonitoringService;

    private Path monitoringDirectory;
    private Path processedDirectory;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directories for testing
        monitoringDirectory = Files.createTempDirectory("monitoring-");
        processedDirectory = Files.createTempDirectory("processed-");

        // Set the directories in the service using reflection
        ReflectionTestUtils.setField(fileMonitoringService, "monitoringDirectory", monitoringDirectory.toString());
        ReflectionTestUtils.setField(fileMonitoringService, "processedDirectory", processedDirectory.toString());
    }

    @Test
    void monitorDirectory_shouldProcessNewFiles() throws IOException {
        // Given
        File file1 = createTestFile(monitoringDirectory, "file1.log");
        File file2 = createTestFile(monitoringDirectory, "file2.log");

        when(fileProcessingService.hasBeenProcessed("file1.log")).thenReturn(false);
        when(fileProcessingService.hasBeenProcessed("file2.log")).thenReturn(false);

        // When
        fileMonitoringService.monitorDirectory();

        // Then
        verify(fileProcessingService).processFile(eq(file1), anyString());
        verify(fileProcessingService).processFile(eq(file2), anyString());
    }

    @Test
    void monitorDirectory_shouldSkipProcessedFiles() throws IOException {
        // Given
        File file1 = createTestFile(monitoringDirectory, "file1.log");
        File file2 = createTestFile(monitoringDirectory, "file2.log");

        when(fileProcessingService.hasBeenProcessed("file1.log")).thenReturn(true);
        when(fileProcessingService.hasBeenProcessed("file2.log")).thenReturn(false);

        // When
        fileMonitoringService.monitorDirectory();

        // Then
        verify(fileProcessingService, never()).processFile(eq(file1), anyString());
        verify(fileProcessingService).processFile(eq(file2), anyString());
    }

    @Test
    void monitorDirectory_shouldHandleEmptyDirectory() {
        // When
        fileMonitoringService.monitorDirectory();

        // Then
        verify(fileProcessingService, never()).processFile(any(), anyString());
    }

    @Test
    void monitorDirectory_shouldCreateDirectoriesIfNotExist() throws IOException {
        // Given
        Files.delete(monitoringDirectory);
        Files.delete(processedDirectory);

        // When
        fileMonitoringService.monitorDirectory();

        // Then
        assertTrue(Files.exists(monitoringDirectory));
        assertTrue(Files.exists(processedDirectory));
        verify(fileProcessingService, never()).processFile(any(), anyString());
    }

    private File createTestFile(Path directory, String fileName) throws IOException {
        Path filePath = directory.resolve(fileName);
        Files.writeString(filePath, "Test content");
        return filePath.toFile();
    }

    private void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true but was false");
        }
    }
}