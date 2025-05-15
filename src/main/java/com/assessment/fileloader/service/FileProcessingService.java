package com.assessment.fileloader.service;

import com.assessment.fileloader.model.CallDetailRecord;
import com.assessment.fileloader.model.CdrLog;
import com.assessment.fileloader.repository.CallDetailRecordRepository;
import com.assessment.fileloader.repository.CdrLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final CallDetailRecordRepository cdrRepository;
    private final CdrLogRepository cdrLogRepository;

    public FileProcessingService(CallDetailRecordRepository cdrRepository, CdrLogRepository cdrLogRepository) {
        this.cdrRepository = cdrRepository;
        this.cdrLogRepository = cdrLogRepository;
    }

    @Transactional
    public void processFile(File file, String processedDirectory) {
        String fileName = file.getName();
        logger.info("Starting to process file: {}", fileName);

        // Check if file has already been processed
        Optional<CdrLog> existingLog = cdrLogRepository.findByFileName(fileName);
        if (existingLog.isPresent() && "COMPLETED".equals(existingLog.get().getStatus())) {
            logger.info("File {} has already been processed. Skipping.", fileName);
            return;
        }

        // Create or update log entry
        CdrLog cdrLog = existingLog.orElse(new CdrLog());
        cdrLog.setFileName(fileName);
        cdrLog.setUploadStartTime(LocalDateTime.now());
        cdrLog.setStatus("PROCESSING");
        cdrLog.setSuccessfulRecords(0);
        cdrLog.setFailedRecords(0);
        cdrLogRepository.save(cdrLog);

        List<CallDetailRecord> records = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    CallDetailRecord record = parseRecord(line);
                    records.add(record);
                    successCount++;

                    // Batch processing to avoid memory issues with large files
                    if (records.size() >= 100) {
                        cdrRepository.saveAll(records);
                        records.clear();
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.error("Error parsing line: {}", line, e);
                }
            }

            // Save any remaining records
            if (!records.isEmpty()) {
                cdrRepository.saveAll(records);
            }

            // Update log entry
            cdrLog.setUploadEndTime(LocalDateTime.now());
            cdrLog.setSuccessfulRecords(successCount);
            cdrLog.setFailedRecords(failCount);
            cdrLog.setStatus("COMPLETED");
            cdrLogRepository.save(cdrLog);

            // Move file to processed directory
            moveFileToProcessedDirectory(file, processedDirectory);

            logger.info("File {} processed successfully. Success: {}, Failed: {}",
                    fileName, successCount, failCount);
        } catch (Exception e) {
            cdrLog.setUploadEndTime(LocalDateTime.now());
            cdrLog.setStatus("FAILED");
            cdrLog.setErrorMessage(e.getMessage());
            cdrLogRepository.save(cdrLog);

            logger.error("Error processing file: {}", fileName, e);
        }
    }

    private CallDetailRecord parseRecord(String line) {
        /*
         * Parses a pipe-delimited call detail record string into a CallDetailRecord object.
         *
         * Note on implementation: This method uses explicit field-by-field parsing rather than a loop
         * for several reasons:
         * 1. Type safety - Each field requires specific type conversion
         * 2. Clarity - Easy to see which index corresponds to which field
         * 3. Maintainability - Simple to modify validation or parsing logic for individual fields
         * 4. Robustness - Gracefully handles records with missing fields
         *
         * @param line The pipe-delimited record string to parse
         * @return A populated CallDetailRecord object
         * @throws RuntimeException if date parsing fails
         */
        String[] fields = line.split("\\|");
        CallDetailRecord record = new CallDetailRecord();

        try {
            // Parse RECORD_DATE (first field)
            record.setRecordDate(LocalDateTime.parse(fields[0], DATE_FORMATTER));

            // Parse numeric fields
            if (fields.length > 1) record.setLSpc(parseIntOrNull(fields[1]));
            if (fields.length > 2) record.setLSsn(parseIntOrNull(fields[2]));
            if (fields.length > 3) record.setLRi(parseIntOrNull(fields[3]));
            if (fields.length > 4) record.setLGtI(parseIntOrNull(fields[4]));
            if (fields.length > 5) record.setLGtDigits(fields[5]);
            if (fields.length > 6) record.setRSpc(parseIntOrNull(fields[6]));
            if (fields.length > 7) record.setRSsn(parseIntOrNull(fields[7]));
            if (fields.length > 8) record.setRRi(parseIntOrNull(fields[8]));
            if (fields.length > 9) record.setRGtI(parseIntOrNull(fields[9]));
            if (fields.length > 10) record.setRGtDigits(fields[10]);
            if (fields.length > 11) record.setServiceCode(fields[11]);
            if (fields.length > 12) record.setOrNature(parseIntOrNull(fields[12]));
            if (fields.length > 13) record.setOrPlan(parseIntOrNull(fields[13]));
            if (fields.length > 14) record.setOrDigits(fields[14]);
            if (fields.length > 15) record.setDeNature(parseIntOrNull(fields[15]));
            if (fields.length > 16) record.setDePlan(parseIntOrNull(fields[16]));
            if (fields.length > 17) record.setDeDigits(fields[17]);
            if (fields.length > 18) record.setIsdnNature(parseIntOrNull(fields[18]));
            if (fields.length > 19) record.setIsdnPlan(parseIntOrNull(fields[19]));
            if (fields.length > 20) record.setMsisdn(fields[20]);
            if (fields.length > 21) record.setVlrNature(parseIntOrNull(fields[21]));
            if (fields.length > 22) record.setVlrPlan(parseIntOrNull(fields[22]));
            if (fields.length > 23) record.setVlrDigits(fields[23]);
            if (fields.length > 24) record.setImsi(fields[24]);
            if (fields.length > 25) record.setStatus(fields[25]);
            if (fields.length > 26) record.setType(fields[26]);

            // Parse TSTAMP
            if (fields.length > 27) record.setTstamp(LocalDateTime.parse(fields[27], TIMESTAMP_FORMATTER));

            // Parse remaining fields
            if (fields.length > 28) record.setLocalDialogId(parseLongOrNull(fields[28]));
            if (fields.length > 29) record.setRemoteDialogId(parseLongOrNull(fields[29]));
            if (fields.length > 30) record.setDialogDuration(parseLongOrNull(fields[30]));
            if (fields.length > 31) record.setUssdString(fields[31]);
            if (fields.length > 32) record.setRecordId(fields[32]);

        } catch (DateTimeParseException e) {
            logger.error("Error parsing date in record: {}", line, e);
            throw new RuntimeException("Error parsing date in record", e);
        }

        return record;
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void moveFileToProcessedDirectory(File file, String processedDirectory) throws IOException {
        Path source = file.toPath();
        Path target = Paths.get(processedDirectory, file.getName());

        // Create processed directory if it doesn't exist
        Files.createDirectories(Paths.get(processedDirectory));

        // Move file to processed directory
        Files.move(source, target);
        logger.info("Moved file {} to processed directory", file.getName());
    }
}
