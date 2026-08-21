package com.example.wasaas.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.ipAddress = :ip AND la.attemptedAt > :since")
    long countRecentFailures(String email, String ip, Instant since);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.email = :email AND la.ipAddress = :ip")
    void deleteByEmailAndIp(String email, String ip);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    void deleteOlderThan(Instant before);
}
