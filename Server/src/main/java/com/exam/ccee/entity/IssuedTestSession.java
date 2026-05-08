package com.exam.ccee.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class IssuedTestSession {

    @Id
    private String sessionId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime consumedAt;

    @ElementCollection
    @CollectionTable(name = "issued_test_session_questions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "question_order")
    @Column(name = "question_id", nullable = false)
    private List<Integer> questionIds = new ArrayList<>();
}
