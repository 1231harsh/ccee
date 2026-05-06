package com.exam.ccee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Data;

@Embeddable
@Data
public class AttemptQuestionReview {

    private int questionId;

    @Lob
    private String questionText;

    private String subject;
    private String topic;

    private Integer selectedOption;

    @Lob
    private String selectedAnswer;

    private int correctOption;

    @Lob
    private String correctAnswer;

    private boolean correct;
}
