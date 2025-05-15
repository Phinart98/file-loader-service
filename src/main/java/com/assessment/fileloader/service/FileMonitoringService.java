package com.assessment.fileloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
public class FileMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);

    @Value("${file.monitoring.directory}")
    private String monitoringDirectory;

    @Value("${file.monitoring.processed-directory}")
    private String processedDirectory;

    private final FileProcessingService fileProcessingService;

    public FileMonitoringService(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @Scheduled(fixedRateString = "${file.monitoring.interval}")
    public void monitorDirectory() {
        logger.info("Checking directory for new files: {}", monitoringDirectory);

        try {
            // Create directories if they don't exist
            createDirectoriesIfNotExist();

            // Get all files in the monitoring directory
            File directory = new File(monitoringDirectory);
            File[] files = directory.listFiles();

            if (files == null || files.length == 0) {
                logger.info("No files found in directory: {}", monitoringDirectory);
                return;
            }

            // Process each file
            List<File> fileList = Arrays.stream(files)
                    .filter(File::isFile)
                    .toList();

            logger.info("Found {} files to process", fileList.size());

            for (File file : fileList) {
                fileProcessingService.processFile(file, processedDirectory);
            }

        } catch (IOException e) {
            logger.error("Error monitoring directory: {}", monitoringDirectory, e);
        }
    }

    private void createDirectoriesIfNotExist() throws IOException {
        Path monitoringPath = Paths.get(monitoringDirectory);
        Path processedPath = Paths.get(processedDirectory);

        if (!Files.exists(monitoringPath)) {
            logger.info("Creating monitoring directory: {}", monitoringDirectory);
            Files.createDirectories(monitoringPath);
        }

        if (!Files.exists(processedPath)) {
            logger.info("Creating processed directory: {}", processedDirectory);
            Files.createDirectories(processedPath);
        }
    }
}
