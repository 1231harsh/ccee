package com.exam.ccee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class AttemptQuestionReview {

    private int questionId;

    @Column(length = 1000)
    private String questionText;

    private String subject;
    private String topic;

    private Integer selectedOption;

    @Column(length = 1000)
    private String selectedAnswer;

    private int correctOption;

    @Column(length = 1000)
    private String correctAnswer;

    private boolean correct;
}
