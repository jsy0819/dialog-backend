package com.dialog.todo.domain;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.dialog.meeting.domain.Meeting;
import com.dialog.user.domain.MeetUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task") // 테이블명 명시
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Todo {

    // 1. id (PK, BIGINT, AI)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. title (VARCHAR(500), NN)
    @Column(nullable = false, length = 500)
    private String title;

    // 3. description (TEXT)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // 4. user_id (FK, BIGINT, NN) - 담당자 아님, 소유자(작성자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MeetUser user;

    // 5. assignee_name (VARCHAR(100)) - 실제 수행 담당자 이름 (선택)
    @Column(name = "assignee_name", length = 100)
    private String assigneeName;

    // 6. due_date (DATE)
    @Column(name = "due_date")
    private LocalDate dueDate;

    // 7. status (ENUM, NN, Default: 'TODO')
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default // 빌더 패턴 사용 시 기본값 적용을 위해 필수
    private TodoStatus status = TodoStatus.TODO;

    // 8. meeting_id (FK, BIGINT) - 회의에서 파생된 경우 연결 (Nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    // 9. source (ENUM, NN, Default: 'MANUAL')
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TodoSource source = TodoSource.MANUAL;

    // 10. extracted_from_text (TEXT) - AI 추출 시 원문 문장
    @Lob
    @Column(name = "extracted_from_text", columnDefinition = "TEXT")
    private String extractedFromText;

    // 11. created_at (TIMESTAMP, NN, Default: NOW)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 12. updated_at (TIMESTAMP, NN, Default: NOW ON UPDATE)
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 정보 수정
    public void updateInfo(String title, String description, LocalDate dueDate, String assigneeName) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (dueDate != null) this.dueDate = dueDate;
        if (assigneeName != null) this.assigneeName = assigneeName;
    }

    // 완료 처리
    public void complete() {
        this.status = TodoStatus.COMPLETED;
    }

    // 미완료 처리 (취소)
    public void markAsTodo() {
        this.status = TodoStatus.TODO;
    }
    
    // 상태 토글 (완료 <-> 미완료)
    public void toggleStatus() {
        if (this.status == TodoStatus.COMPLETED) {
            this.status = TodoStatus.TODO;
        } else {
            this.status = TodoStatus.COMPLETED;
        }
    }
}