package com.princeramteke.resumeai.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("SELECT rt FROM RefreshToken rt " +
           "WHERE rt.tokenHash = :tokenHash AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    Optional<RefreshToken> findByTokenHashAndNotRevokedAndNotExpired(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now);

    @Query("DELETE FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL OR rt.expiresAt < :now")
    void deleteExpiredAndRevoked(@Param("now") Instant now);
}
