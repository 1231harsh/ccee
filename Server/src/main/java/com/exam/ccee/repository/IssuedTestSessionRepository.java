package com.exam.ccee.repository;

import com.exam.ccee.entity.IssuedTestSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface IssuedTestSessionRepository extends JpaRepository<IssuedTestSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IssuedTestSession> findBySessionIdAndUserId(String sessionId, String userId);
}
