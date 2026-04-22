package com.exam.ccee.dto;

import java.util.List;

public record TestQuestionResponse(
        int id,
        String question,
        List<String> options,
        String subject,
        String topic
) {
}
