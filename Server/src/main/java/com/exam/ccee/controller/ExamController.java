package com.exam.ccee.controller;

import com.exam.ccee.dto.TestQuestionResponse;
import com.exam.ccee.entity.Question;
import com.exam.ccee.entity.TestAttempt;
import com.exam.ccee.jwt.JwtUtil;
import com.exam.ccee.repository.TestAttemptRepository;
import com.exam.ccee.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/exam")
@CrossOrigin
public class ExamController {
    private static final List<String> SUBJECT_ORDER = List.of("JAVA", "DBMS", "CPP", "WEB", "DSA");
    private static final int SUBJECT_TEST_MAX_SCORE = 45;
    private static final int MAX_CCEE_SCORE = SUBJECT_ORDER.size() * SUBJECT_TEST_MAX_SCORE;

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

        return testAttemptRepository.findByUserIdOrderByTimestampAsc(username);
    }

    @GetMapping("/analysis-summary")
    public Map<String, Object> getSummary(@RequestHeader("Authorization") String token) {

        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));

        List<TestAttempt> attempts = testAttemptRepository.findByUserIdOrderByTimestampAsc(username);

        Map<String, Integer> weakTopics = buildWeakTopicSummary(attempts);
        int totalScore = 0;

        for (TestAttempt t : attempts) {
            totalScore += t.getScore();
        }

        double avgScore = attempts.isEmpty() ? 0 : (double) totalScore / attempts.size();
        Map<String, List<TestAttempt>> attemptsBySubject = groupAttemptsBySubject(attempts);
        Map<String, Object> subjectSummaries = buildSubjectSummaries(attemptsBySubject);
        Map<String, Object> latestComposite = buildLatestComposite(attemptsBySubject);

        return Map.of(
                "attempts", attempts.size(),
                "averageScore", avgScore,
                "weakTopics", weakTopics,
                "subjectSummaries", subjectSummaries,
                "latestComposite", latestComposite,
                "maxCceeScore", MAX_CCEE_SCORE
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

    private Map<String, List<TestAttempt>> groupAttemptsBySubject(List<TestAttempt> attempts) {
        Map<String, List<TestAttempt>> grouped = new LinkedHashMap<>();

        for (String subject : SUBJECT_ORDER) {
            grouped.put(subject, new ArrayList<>());
        }

        for (TestAttempt attempt : attempts) {
            String subjectKey = attempt.getSubject() == null ? "" : attempt.getSubject().trim().toUpperCase();
            grouped.computeIfAbsent(subjectKey, ignored -> new ArrayList<>()).add(attempt);
        }

        for (List<TestAttempt> subjectAttempts : grouped.values()) {
            subjectAttempts.sort(Comparator.comparing(TestAttempt::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return grouped;
    }

    private Map<String, Object> buildSubjectSummaries(Map<String, List<TestAttempt>> attemptsBySubject) {
        Map<String, Object> summaries = new LinkedHashMap<>();

        for (Map.Entry<String, List<TestAttempt>> entry : attemptsBySubject.entrySet()) {
            String subject = entry.getKey();
            List<TestAttempt> subjectAttempts = entry.getValue();

            if (subjectAttempts.isEmpty()) {
                summaries.put(subject, Map.of(
                        "attempts", 0,
                        "averageScore", 0,
                        "latestScore", 0,
                        "latestTotal", SUBJECT_TEST_MAX_SCORE,
                        "bestScore", 0,
                        "growth", 0,
                        "weakTopics", Map.of()
                ));
                continue;
            }

            int totalScore = 0;
            int bestScore = 0;
            Map<String, Integer> weakTopics = new HashMap<>();

            for (TestAttempt attempt : subjectAttempts) {
                totalScore += attempt.getScore();
                bestScore = Math.max(bestScore, attempt.getScore());

                if (attempt.getWrongPerTopic() == null) {
                    continue;
                }

                for (Map.Entry<String, Integer> topicEntry : attempt.getWrongPerTopic().entrySet()) {
                    weakTopics.put(
                            topicEntry.getKey(),
                            weakTopics.getOrDefault(topicEntry.getKey(), 0) + topicEntry.getValue()
                    );
                }
            }

            TestAttempt latest = subjectAttempts.get(subjectAttempts.size() - 1);
            TestAttempt previous = subjectAttempts.size() > 1 ? subjectAttempts.get(subjectAttempts.size() - 2) : null;
            int growth = previous == null ? 0 : latest.getScore() - previous.getScore();

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("attempts", subjectAttempts.size());
            summary.put("averageScore", (double) totalScore / subjectAttempts.size());
            summary.put("latestScore", latest.getScore());
            summary.put("latestTotal", latest.getTotal());
            summary.put("latestTimestamp", latest.getTimestamp());
            summary.put("bestScore", bestScore);
            summary.put("growth", growth);
            summary.put("weakTopics", weakTopics);
            summaries.put(subject, summary);
        }

        return summaries;
    }

    private Map<String, Object> buildLatestComposite(Map<String, List<TestAttempt>> attemptsBySubject) {
        Map<String, Object> latestPerSubject = new LinkedHashMap<>();
        int latestCompositeScore = 0;
        int latestCompositeTotalAttempted = 0;
        int completedSubjects = 0;

        for (String subject : SUBJECT_ORDER) {
            List<TestAttempt> subjectAttempts = attemptsBySubject.getOrDefault(subject, List.of());
            if (subjectAttempts.isEmpty()) {
                latestPerSubject.put(subject, null);
                continue;
            }

            TestAttempt latest = subjectAttempts.get(subjectAttempts.size() - 1);
            latestCompositeScore += latest.getScore();
            latestCompositeTotalAttempted += latest.getTotal();
            completedSubjects++;

            latestPerSubject.put(subject, Map.of(
                    "score", latest.getScore(),
                    "total", latest.getTotal(),
                    "timestamp", latest.getTimestamp()
            ));
        }

        Map<String, Object> composite = new LinkedHashMap<>();
        composite.put("score", latestCompositeScore);
        composite.put("attemptedTotal", latestCompositeTotalAttempted);
        composite.put("maxScore", MAX_CCEE_SCORE);
        composite.put("completedSubjects", completedSubjects);
        composite.put("latestPerSubject", latestPerSubject);
        return composite;
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



