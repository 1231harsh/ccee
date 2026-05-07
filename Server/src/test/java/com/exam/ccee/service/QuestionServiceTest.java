package com.exam.ccee.service;

import com.exam.ccee.entity.AttemptQuestionReview;
import com.exam.ccee.entity.Question;
import com.exam.ccee.entity.TestAttempt;
import com.exam.ccee.repository.TestAttemptRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    @Test
    void generateTestForUserPrefersFreshQuestionsFromRecentAttempts() {
        TestAttemptRepository repository = mock(TestAttemptRepository.class);
        when(repository.findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc("user1", "JAVA"))
                .thenReturn(List.of(buildAttempt(
                        LocalDateTime.now(),
                        400, 401, 402, 403, 404
                )));

        QuestionService service = new QuestionService(repository);

        List<Question> generated = service.generateTestForUser("user1", "JAVA");
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
        when(repository.findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc("user1", "JAVA"))
                .thenReturn(List.of(
                        buildAttempt(LocalDateTime.now(), 400, 401, 402, 403, 404, 405, 406),
                        buildAttempt(LocalDateTime.now().minusDays(1), 407, 408, 409, 410, 411, 412, 413),
                        buildAttempt(LocalDateTime.now().minusDays(2), 414, 415, 416, 417, 418, 419)
                ));

        QuestionService service = new QuestionService(repository);

        List<Question> generated = service.generateTestForUser("user1", "JAVA");
        List<Question> generatedJavaSyntaxQuestions = generated.stream()
                .filter(q -> "JAVA".equals(q.subject))
                .filter(q -> "Java Syntax".equals(q.topic))
                .toList();

        assertEquals(45, generated.size());
        assertEquals(5, generatedJavaSyntaxQuestions.size());
        assertTrue(generatedJavaSyntaxQuestions.stream().allMatch(q -> q.id >= 400 && q.id <= 419));
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
}
