package com.princeramteke.resumeai.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, Long> {

    @Query("""
            SELECT oec FROM OAuthExchangeCode oec
            WHERE oec.codeHash = :codeHash
              AND oec.usedAt IS NULL
              AND oec.expiresAt > :now
            """)
    Optional<OAuthExchangeCode> findByCodeHashAndNotUsedAndNotExpired(
            @Param("codeHash") String codeHash,
            @Param("now") Instant now);
}
