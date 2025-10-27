package com.dialog.participant.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.dialog.meeting.domain.Meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Table(name = "participant",
		uniqueConstraints = @UniqueConstraint(name = "uq_meeting_speaker", columnNames = {"meeting_id", "speaker_id"})
)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "speaker_id", length = 50, nullable = false)
    private String speakerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
