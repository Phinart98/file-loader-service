package com.assessment.fileloader.service;

import com.assessment.fileloader.model.CallDetailRecord;
import com.assessment.fileloader.model.CdrLog;
import com.assessment.fileloader.repository.CallDetailRecordRepository;
import com.assessment.fileloader.repository.CdrLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileProcessingService {

    private final CallDetailRecordRepository callDetailRecordRepository;
    private final CdrLogRepository cdrLogRepository;

    @Transactional
    public void processFile(File file, String processedDirectory) {
        log.info("Processing file: {}", file.getName());

        CdrLog cdrLog = new CdrLog();
        cdrLog.setFileName(file.getName());
        cdrLog.setUploadStartTime(LocalDateTime.now());

        int successCount = 0;
        int failedCount = 0;

        List<CallDetailRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    CallDetailRecord record = parseRecord(line);
                    records.add(record);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error parsing record: {}", line, e);
                    failedCount++;
                }
            }

            // Save all records in batch
            if (!records.isEmpty()) {
                callDetailRecordRepository.saveAll(records);
            }

            cdrLog.setSuccessCount(successCount);
            cdrLog.setFailedCount(failedCount);
            cdrLog.setUploadEndTime(LocalDateTime.now());
            cdrLogRepository.save(cdrLog);

            // Move the file to processed directory
            moveFileToProcessedDirectory(file, processedDirectory);

            log.info("File processed: {}. Success: {}, Failed: {}", file.getName(), successCount, failedCount);
        } catch (IOException e) {
            log.error("Error processing file: {}", file.getName(), e);
            cdrLog.setFailedCount(failedCount);
            cdrLog.setSuccessCount(successCount);
            cdrLog.setUploadEndTime(LocalDateTime.now());
            cdrLogRepository.save(cdrLog);
        }
    }

    private void moveFileToProcessedDirectory(File file, String processedDirectory) {
        try {
            Path source = file.toPath();
            Path target = Paths.get(processedDirectory, file.getName());

            // Close any open file handles by forcing a garbage collection
            System.gc();

            // Add a small delay to ensure file handles are released
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Try to move the file
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Moved file to processed directory: {}", target);
            } catch (IOException e) {
                // If moving fails, try to copy and then delete
                log.warn("Could not move file, attempting to copy instead: {}", file.getName());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                // Try to delete the original file, but don't fail if it doesn't work
                try {
                    Files.delete(source);
                } catch (IOException deleteException) {
                    log.warn("Could not delete original file after copying: {}", file.getName());
                }

                log.info("Copied file to processed directory: {}", target);
            }
        } catch (IOException e) {
            log.error("Error moving file to processed directory: {}", file.getName(), e);
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

        // Parse timestamp in format "2023-08-18 10:00:00,024"
        String recordDateStr = fields[0];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
        LocalDateTime recordDate = LocalDateTime.parse(recordDateStr, formatter);
        record.setRecordDate(recordDate);

        // Parse other fields
        record.setLSpc(parseIntOrNull(fields[1]));
        record.setLSsn(parseIntOrNull(fields[2]));
        record.setLRi(parseIntOrNull(fields[3]));
        record.setLGtI(parseIntOrNull(fields[4]));
        record.setLGtDigits(fields[5]);
        record.setRSpc(parseIntOrNull(fields[6]));
        record.setRSsn(parseIntOrNull(fields[7]));
        record.setRRi(parseIntOrNull(fields[8]));
        record.setRGtI(parseIntOrNull(fields[9]));
        record.setRGtDigits(fields[10]);
        record.setServiceCode(fields[11]);
        record.setOrNature(parseIntOrNull(fields[12]));
        record.setOrPlan(parseIntOrNull(fields[13]));
        record.setOrDigits(fields[14]);
        record.setDeNature(parseIntOrNull(fields[15]));
        record.setDePlan(parseIntOrNull(fields[16]));
        record.setDeDigits(fields[17]);
        record.setIsdnNature(parseIntOrNull(fields[18]));
        record.setIsdnPlan(parseIntOrNull(fields[19]));
        record.setMsisdn(fields[20]);

        // Handle optional VLR fields
        if (fields.length > 21) record.setVlrNature(parseIntOrNull(fields[21]));
        if (fields.length > 22) record.setVlrPlan(parseIntOrNull(fields[22]));
        if (fields.length > 23) record.setVlrDigits(fields[23]);

        // Handle IMSI field
        if (fields.length > 24) record.setImsi(fields[24]);

        // Required fields
        if (fields.length > 25) record.setStatus(fields[25]);
        if (fields.length > 26) record.setType(fields[26]);

        // Parse timestamp in format "2023-08-18 10:00:00.024"
        if (fields.length > 27) {
            String tstampStr = fields[27];
            DateTimeFormatter tstampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime tstamp = LocalDateTime.parse(tstampStr, tstampFormatter);
            record.setTstamp(tstamp);
        }

        // Parse remaining fields
        if (fields.length > 28) record.setLocalDialogId(parseLongOrNull(fields[28]));
        if (fields.length > 29) record.setRemoteDialogId(parseLongOrNull(fields[29]));
        if (fields.length > 30) record.setDialogDuration(parseLongOrNull(fields[30]));
        if (fields.length > 31) record.setUssdString(fields[31]);
        if (fields.length > 32) record.setRecordId(fields[32]);

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
}
