package com.dialog.recording.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.meeting.domain.Meeting;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recording")
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@AllArgsConstructor 
@Builder(toBuilder = true) 
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;
    
    @Column(name = "audio_file_url", length = 1000)
    private String audioFileUrl;
    
    @Column(name = "audio_file_size")
    private Long audioFileSize;
    
    @Column(name = "audio_format", length = 20)
    private String audioFormat;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 녹음파일 저장 시 호출 */
    public void saveAudioFile(String url, Long fileSize, String format, Integer duration) {
        this.audioFileUrl = url;
        this.audioFileSize = fileSize;
        this.audioFormat = format;
        this.durationSeconds = duration;
    }

    /** 업로드 완료 처리 (null 허용) */
    public void completeUpload(String url, Long fileSize, String format, Integer duration) {
        if (url != null && !url.isBlank()) {
            this.audioFileUrl = url;
        }
        if (fileSize != null) {
            this.audioFileSize = fileSize;
        }
        if (format != null) {
            this.audioFormat = format;
        }
        if (duration != null) {
            this.durationSeconds = duration;
        }
    }

    /** 오디오 파일 존재 여부 */
    public boolean hasAudioFile() {
        return this.audioFileUrl != null && !this.audioFileUrl.isBlank();
    }
}