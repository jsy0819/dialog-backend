package com.dialog.token.domain;

import java.time.LocalDateTime;



import com.dialog.user.domain.MeetUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 리프레시 토큰 PK

    @Column(nullable = false, length = 512, unique = true)
    private String refreshToken; // 실제 리프레시 토큰 문자열

    @Column(nullable = false)
    private LocalDateTime issuedAt; // 발급 시각

    @Column(nullable = false)
    private LocalDateTime expiresAt; // 만료 시각

    @Column(nullable = false)
    private boolean revoked; // 토큰 만료/사용 불가 상태

    // MeetUser와 다대일(N:1) 관계 (user_id FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MeetUser user;

    @Builder
    public RefreshToken(Long id, String refreshToken, LocalDateTime issuedAt,
                        LocalDateTime expiresAt, boolean revoked, MeetUser user) {
        this.id = id;
        this.refreshToken = refreshToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.user = user;
    }
}
