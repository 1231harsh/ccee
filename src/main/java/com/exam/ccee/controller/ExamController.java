package com.exam.ccee.controller;

import com.exam.ccee.dto.TestQuestionResponse;
import com.exam.ccee.entity.Question;
import com.exam.ccee.entity.TestAttempt;
import com.exam.ccee.jwt.JwtUtil;
import com.exam.ccee.repository.TestAttemptRepository;
import com.exam.ccee.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@CrossOrigin
public class ExamController {

    private final QuestionService questionService;
    private final JwtUtil jwtUtil;
    private final TestAttemptRepository testAttemptRepository;

    public ExamController(
            QuestionService questionService,
            JwtUtil jwtUtil,
            TestAttemptRepository testAttemptRepository) {
        this.testAttemptRepository = testAttemptRepository;
        this.questionService = questionService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/start-test/{subject}")
    public List<TestQuestionResponse> startTest(
            @RequestHeader("Authorization") String token,
            @PathVariable String subject) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));

        return questionService.generateTestForUser(username, subject).stream()
                .map(this::toTestQuestionResponse)
                .toList();
    }

    @PostMapping("/submit-test")
    public Map<String, Object> submitTest(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> payload) {

        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));

        Map<Integer, Integer> answers = parseAnswers(payload.get("answers"));

        String subject = (String) payload.get("subject");

        return questionService.evaluateAndSave(username, subject, answers);
    }

    @GetMapping("/analysis")
    public List<TestAttempt> getAnalysis(@RequestHeader("Authorization") String token) {

        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));

        return testAttemptRepository.findByUserId(username);
    }

    @GetMapping("/analysis-summary")
    public Map<String, Object> getSummary(@RequestHeader("Authorization") String token) {

        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));

        List<TestAttempt> attempts = testAttemptRepository.findByUserId(username);

        Map<String, Integer> weakTopics = buildWeakTopicSummary(attempts);
        int totalScore = 0;

        for (TestAttempt t : attempts) {
            totalScore += t.getScore();
        }

        double avgScore = attempts.isEmpty() ? 0 : (double) totalScore / attempts.size();

        return Map.of(
                "attempts", attempts.size(),
                "averageScore", avgScore,
                "weakTopics", weakTopics
        );
    }

    private Map<String, Integer> buildWeakTopicSummary(List<TestAttempt> attempts) {
        Map<String, Integer> weakTopics = new HashMap<>();

        for (TestAttempt t : attempts) {
            if (t.getWrongPerTopic() == null) {
                continue;
            }

            for (String topic : t.getWrongPerTopic().keySet()) {
                weakTopics.put(topic,
                        weakTopics.getOrDefault(topic, 0) + t.getWrongPerTopic().get(topic));
            }
        }

        return weakTopics;
    }

    private TestQuestionResponse toTestQuestionResponse(Question question) {
        return new TestQuestionResponse(
                question.id,
                question.question,
                question.options,
                question.subject,
                question.topic
        );
    }

    private Map<Integer, Integer> parseAnswers(Object answersPayload) {
        if (!(answersPayload instanceof Map<?, ?> rawAnswers)) {
            throw new IllegalArgumentException("Answers payload is invalid");
        }

        Map<Integer, Integer> parsedAnswers = new HashMap<>();

        for (Map.Entry<?, ?> entry : rawAnswers.entrySet()) {
            int questionId = Integer.parseInt(String.valueOf(entry.getKey()));
            int selectedOption = Integer.parseInt(String.valueOf(entry.getValue()));
            parsedAnswers.put(questionId, selectedOption);
        }

        return parsedAnswers;
    }
}



