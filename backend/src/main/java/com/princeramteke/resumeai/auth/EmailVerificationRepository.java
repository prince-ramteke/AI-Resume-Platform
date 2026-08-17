package com.princeramteke.resumeai.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Query("""
            SELECT ev FROM EmailVerification ev
            WHERE ev.user.id = :userId
              AND ev.usedAt IS NULL
              AND ev.expiresAt > :now
              AND ev.attemptCount < :maxAttempts
            ORDER BY ev.createdAt DESC
            LIMIT 1
            """)
    Optional<EmailVerification> findLatestActiveByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts);

    @Query("""
            SELECT ev FROM EmailVerification ev
            WHERE ev.user.id = :userId
            ORDER BY ev.createdAt DESC
            LIMIT 1
            """)
    Optional<EmailVerification> findMostRecentByUserId(@Param("userId") Long userId);
}
