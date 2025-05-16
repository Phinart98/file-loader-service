package com.assessment.fileloader.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cdr_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdrLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "upload_start_time", nullable = false)
    private LocalDateTime uploadStartTime;

    @Column(name = "upload_end_time")
    private LocalDateTime uploadEndTime;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failed_count")
    private Integer failedCount;
}
