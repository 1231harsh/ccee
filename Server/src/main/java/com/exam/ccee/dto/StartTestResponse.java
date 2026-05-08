package com.exam.ccee.dto;

import java.util.List;

public record StartTestResponse(
        String sessionId,
        String subject,
        List<TestQuestionResponse> questions
) {
}
