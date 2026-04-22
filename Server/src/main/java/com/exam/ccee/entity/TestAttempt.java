package com.exam.ccee.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Data
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;   // username from JWT
    private String subject;

    private int score;
    private int total;

    private LocalDateTime timestamp;

    // store topic-wise mistakes
    @ElementCollection
    @CollectionTable(name = "wrong_topics")
    @MapKeyColumn(name = "topic")
    @Column(name = "wrong_count")
    private Map<String, Integer> wrongPerTopic;

    @ElementCollection
    @CollectionTable(name = "attempt_question_reviews", joinColumns = @JoinColumn(name = "attempt_id"))
    @OrderColumn(name = "question_order")
    private List<AttemptQuestionReview> questionReviews = new ArrayList<>();
}
