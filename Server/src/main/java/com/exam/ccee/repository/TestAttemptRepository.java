package com.exam.ccee.repository;

import com.exam.ccee.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    List<TestAttempt> findByUserIdOrderByTimestampAsc(String userId);

    List<TestAttempt> findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc(String userId, String subject);

}
