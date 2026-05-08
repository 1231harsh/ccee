package com.exam.ccee.service;

import com.exam.ccee.entity.AttemptQuestionReview;
import com.exam.ccee.entity.IssuedTestSession;
import com.exam.ccee.entity.Question;
import com.exam.ccee.entity.TestAttempt;
import com.exam.ccee.exception.ApiException;
import com.exam.ccee.repository.IssuedTestSessionRepository;
import com.exam.ccee.repository.TestAttemptRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    @Test
    void generateTestForUserPrefersFreshQuestionsFromRecentAttempts() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);
        when(repository.findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc("user1", "JAVA"))
                .thenReturn(List.of(buildAttempt(
                        LocalDateTime.now(),
                        400, 401, 402, 403, 404
                )));
        when(issuedTestSessionRepository.save(any(IssuedTestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionService service = new QuestionService(repository, issuedTestSessionRepository);

        List<Question> generated = service.generateTestForUser("user1", "JAVA").questions();
        List<Integer> generatedJavaSyntaxIds = generated.stream()
                .filter(q -> "JAVA".equals(q.subject))
                .filter(q -> "Java Syntax".equals(q.topic))
                .map(q -> q.id)
                .toList();

        assertEquals(45, generated.size());
        assertEquals(5, generatedJavaSyntaxIds.size());
        assertTrue(generatedJavaSyntaxIds.stream().noneMatch(id -> id >= 400 && id <= 404));
    }

    @Test
    void generateTestForUserFallsBackToRecentQuestionsWhenTopicPoolIsExhausted() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);
        when(repository.findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc("user1", "JAVA"))
                .thenReturn(List.of(
                        buildAttempt(LocalDateTime.now(), 400, 401, 402, 403, 404, 405, 406),
                        buildAttempt(LocalDateTime.now().minusDays(1), 407, 408, 409, 410, 411, 412, 413),
                        buildAttempt(LocalDateTime.now().minusDays(2), 414, 415, 416, 417, 418, 419)
                ));
        when(issuedTestSessionRepository.save(any(IssuedTestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionService service = new QuestionService(repository, issuedTestSessionRepository);

        List<Question> generated = service.generateTestForUser("user1", "JAVA").questions();
        List<Question> generatedJavaSyntaxQuestions = generated.stream()
                .filter(q -> "JAVA".equals(q.subject))
                .filter(q -> "Java Syntax".equals(q.topic))
                .toList();

        assertEquals(45, generated.size());
        assertEquals(5, generatedJavaSyntaxQuestions.size());
        assertTrue(generatedJavaSyntaxQuestions.stream().allMatch(q -> q.id >= 400 && q.id <= 419));
    }

    @Test
    void evaluateAndSaveCanRecoverSubmissionWhenActiveTestStateIsMissing() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);
        when(repository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issuedTestSessionRepository.save(any(IssuedTestSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedTestSession issuedTestSession = buildIssuedTestSession("session-1", "user1", "JAVA", 400, 401, 402, 403, 404);
        when(issuedTestSessionRepository.findBySessionIdAndUserId("session-1", "user1"))
                .thenReturn(Optional.of(issuedTestSession));

        QuestionService service = new QuestionService(repository, issuedTestSessionRepository);

        Map<String, Object> result = service.evaluateAndSave(
                "user1",
                "session-1",
                Map.of(400, 1, 401, 2)
        );

        assertEquals(2, result.get("score"));
        assertEquals(5, result.get("total"));
    }

    @Test
    void evaluateAndSaveRejectsReplayForConsumedSession() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);

        IssuedTestSession issuedTestSession = buildIssuedTestSession("session-2", "user1", "JAVA", 400, 401, 402, 403, 404);
        issuedTestSession.setConsumedAt(LocalDateTime.now());
        when(issuedTestSessionRepository.findBySessionIdAndUserId("session-2", "user1"))
                .thenReturn(Optional.of(issuedTestSession));

        QuestionService service = new QuestionService(repository, issuedTestSessionRepository);

        ApiException ex = assertThrows(ApiException.class, () ->
                service.evaluateAndSave("user1", "session-2", Map.of(400, 1))
        );

        assertEquals("This test has already been submitted.", ex.getMessage());
    }

    @Test
    void generateTestForUserRejectsUnsupportedSubject() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);
        QuestionService service = new QuestionService(repository, issuedTestSessionRepository);

        ApiException ex = assertThrows(ApiException.class, () ->
                service.generateTestForUser("user1", "PYTHON")
        );

        assertEquals("Unsupported subject: PYTHON", ex.getMessage());
    }

    @Test
    void generateTestForUserRejectsEmptyQuestionSet() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        IssuedTestSessionRepository issuedTestSessionRepository = mock(IssuedTestSessionRepository.class);
        QuestionService service = new QuestionService(repository, issuedTestSessionRepository) {
            @Override
            public List<Question> loadQuestions() {
                return List.of();
            }
        };

        ApiException ex = assertThrows(ApiException.class, () ->
                service.generateTestForUser("user1", "JAVA")
        );

        assertEquals("No questions are available for JAVA right now. Please try again shortly.", ex.getMessage());
    }

    private TestAttempt buildAttempt(LocalDateTime timestamp, int... questionIds) {
        TestAttempt attempt = new TestAttempt();
        attempt.setTimestamp(timestamp);

        List<AttemptQuestionReview> reviews = new ArrayList<>();
        for (int questionId : questionIds) {
            AttemptQuestionReview review = new AttemptQuestionReview();
            review.setQuestionId(questionId);
            reviews.add(review);
        }

        attempt.setQuestionReviews(reviews);
        return attempt;
    }

    private IssuedTestSession buildIssuedTestSession(String sessionId, String userId, String subject, int... questionIds) {
        IssuedTestSession issuedTestSession = new IssuedTestSession();
        issuedTestSession.setSessionId(sessionId);
        issuedTestSession.setUserId(userId);
        issuedTestSession.setSubject(subject);
        issuedTestSession.setCreatedAt(LocalDateTime.now());

        List<Integer> ids = new ArrayList<>();
        for (int questionId : questionIds) {
            ids.add(questionId);
        }

        issuedTestSession.setQuestionIds(ids);
        return issuedTestSession;
    }
}
