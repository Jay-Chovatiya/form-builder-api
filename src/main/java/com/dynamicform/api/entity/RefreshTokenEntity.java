package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private UserEntity user;

    @Column(name = "TokenHash", nullable = false, length = 255)
    private String tokenHash;

    @Builder.Default
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "RevokedAt")
    private LocalDateTime revokedAt;

    @Column(name = "ReplacedByTokenId")
    private Long replacedByTokenId;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActive() {
        return !isRevoked() && !isExpired();
    }
}
