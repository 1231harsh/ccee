package com.exam.ccee.service;

import com.exam.ccee.entity.AttemptQuestionReview;
import com.exam.ccee.entity.Question;
import com.exam.ccee.entity.TestAttempt;
import com.exam.ccee.repository.TestAttemptRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private static final int RECENT_ATTEMPT_WINDOW = 3;

    private final List<Question> questions;
    private final TestAttemptRepository testAttemptRepository;
    private final Map<String, List<Question>> activeTests = new ConcurrentHashMap<>();
    private final Random random = new Random();

     QuestionService(TestAttemptRepository testAttemptRepository) {
        this.testAttemptRepository = testAttemptRepository;
        this.questions = loadQuestions();
    }

    public List<Question> loadQuestions() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/questions.json");
            return Arrays.asList(mapper.readValue(is, Question[].class));
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Question> generateTestForUser(String userId, String subject) {
        List<Question> generatedQuestions = generateTest(userId, subject);
        activeTests.put(buildAttemptKey(userId, subject), generatedQuestions);
        return generatedQuestions;
    }

    private List<Question> generateTest(String userId, String subject) {

        Map<String, Integer> blueprint = getBlueprint(subject);
        SelectionHistory history = getSelectionHistory(userId, subject);

        List<Question> subjectQs = questions.stream()
                .filter(q -> q.subject.equalsIgnoreCase(subject))
                .toList();

        Map<String, List<Question>> byTopic = subjectQs.stream()
                .collect(Collectors.groupingBy(q -> q.topic.trim().toLowerCase()));

        List<Question> finalTest = new ArrayList<>();

        for (String topic : blueprint.keySet()) {

            List<Question> pool = byTopic.getOrDefault(topic.trim().toLowerCase(), new ArrayList<>());
            int required = blueprint.get(topic);
            finalTest.addAll(selectQuestionsForTopic(pool, required, history));
        }

        Collections.shuffle(finalTest, random);
        return finalTest;
    }

    private SelectionHistory getSelectionHistory(String userId, String subject) {
        if (userId == null || userId.isBlank()) {
            return new SelectionHistory(Map.of(), Map.of(), Set.of());
        }

        List<TestAttempt> attempts = testAttemptRepository
                .findByUserIdAndSubjectIgnoreCaseOrderByTimestampDesc(userId, subject);

        Map<Integer, Integer> seenCountByQuestionId = new HashMap<>();
        Map<Integer, Integer> lastSeenRankByQuestionId = new HashMap<>();
        Set<Integer> recentQuestionIds = new HashSet<>();

        int attemptRank = 0;
        for (TestAttempt attempt : attempts) {
            attemptRank++;

            if (attempt.getQuestionReviews() == null) {
                continue;
            }

            for (AttemptQuestionReview review : attempt.getQuestionReviews()) {
                int questionId = review.getQuestionId();
                seenCountByQuestionId.merge(questionId, 1, Integer::sum);
                lastSeenRankByQuestionId.putIfAbsent(questionId, attemptRank);

                if (attemptRank <= RECENT_ATTEMPT_WINDOW) {
                    recentQuestionIds.add(questionId);
                }
            }
        }

        return new SelectionHistory(seenCountByQuestionId, lastSeenRankByQuestionId, recentQuestionIds);
    }

    private List<Question> selectQuestionsForTopic(List<Question> pool, int required, SelectionHistory history) {
        if (pool.isEmpty() || required <= 0) {
            return new ArrayList<>();
        }

        List<Question> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool, random);

        Comparator<Question> priorityComparator = buildQuestionPriorityComparator(history);

        List<Question> freshCandidates = shuffledPool.stream()
                .filter(q -> !history.recentQuestionIds().contains(q.id))
                .sorted(priorityComparator)
                .toList();

        List<Question> fallbackCandidates = shuffledPool.stream()
                .filter(q -> history.recentQuestionIds().contains(q.id))
                .sorted(priorityComparator)
                .toList();

        List<Question> selected = new ArrayList<>();
        addUpTo(selected, freshCandidates, required);
        addUpTo(selected, fallbackCandidates, required);
        return selected;
    }

    private Comparator<Question> buildQuestionPriorityComparator(SelectionHistory history) {
        return Comparator
                .comparingInt((Question question) -> history.seenCountByQuestionId().getOrDefault(question.id, 0))
                .thenComparing(
                        Comparator.comparingInt(
                                (Question question) -> history.lastSeenRankByQuestionId()
                                        .getOrDefault(question.id, Integer.MAX_VALUE)
                        ).reversed()
                );
    }

    private void addUpTo(List<Question> selected, List<Question> candidates, int required) {
        for (Question question : candidates) {
            if (selected.size() >= required) {
                return;
            }

            selected.add(question);
        }
    }

    private Map<String, Integer> getBlueprint(String subject) {

        return switch (subject.toUpperCase()) {
            case "CPP" -> getCPPBlueprint();
            case "DBMS" -> getDBMSBlueprint();
            case "JAVA" -> getJavaBlueprint();
            case "WEB" -> getWebBlueprint();
            case "DSA" -> getDSABlueprint();
            default -> new HashMap<>();
        };
    }

    private Map<String, Integer> getCPPBlueprint() {
        return buildBalancedBlueprint(
                "Structure of C++ Programs",
                "Classes & Objects",
                "Constructors",
                "Inheritance & Polymorphism",
                "Operator Overloading",
                "Function & Class Templates",
                "Exception Handling & Namespaces",
                "Standard Template Library",
                "File Handling",
                "Dynamic Memory & OOP Applications"
        );
    }

    private Map<String, Integer> getDBMSBlueprint() {
        return buildBalancedBlueprint(
                "Database Design",
                "ER Modeling",
                "SQL Commands",
                "Joins & Views",
                "Indexes",
                "Stored Procedures & Functions",
                "Triggers",
                "Transactions & ACID",
                "Query Optimization",
                "PL/SQL, Normalization & NoSQL"
        );
    }

    private Map<String, Integer> getJavaBlueprint() {
        return buildBalancedBlueprint(
                "Java Syntax",
                "Data Types",
                "Classes & Objects",
                "Constructors",
                "Inheritance",
                "Interfaces",
                "Exception Handling",
                "Collections",
                "OOP Principles",
                "Java Practice Patterns"
        );
    }

    private Map<String, Integer> getWebBlueprint() {
        return buildBalancedBlueprint(
                "HTML5",
                "CSS3",
                "Bootstrap",
                "JavaScript ES6",
                "DOM Manipulation",
                "Responsive Web Design",
                "AJAX & JSON",
                "Client-Server Architecture",
                "Frontend Validation",
                "Web Hosting Basics"
        );
    }

    private Map<String, Integer> getDSABlueprint() {
        return buildBalancedBlueprint(
                "Arrays",
                "Linked Lists",
                "Stacks & Queues",
                "Trees",
                "Graphs",
                "Sorting & Searching",
                "Recursion",
                "Greedy & Divide and Conquer",
                "Dynamic Programming",
                "Complexity & Java Patterns"
        );
    }

    private Map<String, Integer> buildBalancedBlueprint(String... topics) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < topics.length; i++) {
            map.put(topics[i], i < 5 ? 5 : 4);
        }

        return map;
    }

//    public Map<String, Object> evaluateTest(Map<Integer, Integer> answers) {
//
//        int score = 0;
//        Map<String, Integer> correctPerTopic = new HashMap<>();
//        Map<String, Integer> wrongPerTopic = new HashMap<>();
//
//        for (Question q : questions) {
//
//            if (!answers.containsKey(q.id)) continue;
//
//            int selected = answers.get(q.id);
//
//            if (selected == q.answer) {
//                score++;
//                correctPerTopic.put(q.topic,
//                        correctPerTopic.getOrDefault(q.topic, 0) + 1);
//            } else {
//                wrongPerTopic.put(q.topic,
//                        wrongPerTopic.getOrDefault(q.topic, 0) + 1);
//            }
//        }
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("score", score);
//        result.put("total", 45);
//        result.put("correctPerTopic", correctPerTopic);
//        result.put("wrongPerTopic", wrongPerTopic);
//
//        return result;
//    }

    public Map<String, Object> evaluateAndSave(String userId, String subject, Map<Integer, Integer> answers) {
        String attemptKey = buildAttemptKey(userId, subject);
        List<Question> issuedQuestions = activeTests.remove(attemptKey);

        if (issuedQuestions == null || issuedQuestions.isEmpty()) {
            throw new IllegalStateException("No active test found for this user and subject");
        }

        int score = 0;
        Map<String, Integer> wrongPerTopic = new HashMap<>();
        List<AttemptQuestionReview> questionReviews = new ArrayList<>();
        Set<Integer> issuedQuestionIds = issuedQuestions.stream()
                .map(q -> q.id)
                .collect(Collectors.toSet());

        for (Question q : issuedQuestions) {
            Integer selected = answers.get(q.id);
            boolean answered = selected != null;
            boolean correct = answered && selected == q.answer;

            if (correct) {
                score++;
            } else if (answered) {
                wrongPerTopic.put(q.topic,
                        wrongPerTopic.getOrDefault(q.topic, 0) + 1);
            }

            questionReviews.add(toQuestionReview(q, selected, correct));
        }

        for (Integer submittedQuestionId : answers.keySet()) {
            if (!issuedQuestionIds.contains(submittedQuestionId)) {
                throw new IllegalArgumentException("Submitted answers contain questions not present in the active test");
            }
        }

        int total = issuedQuestions.size();

        TestAttempt attempt = new TestAttempt();
        attempt.setUserId(userId);
        attempt.setSubject(subject);
        attempt.setScore(score);
        attempt.setTotal(total);
        attempt.setTimestamp(LocalDateTime.now());
        attempt.setWrongPerTopic(wrongPerTopic);
        attempt.setQuestionReviews(questionReviews);

        testAttemptRepository.save(attempt);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("total", total);
        result.put("wrongPerTopic", wrongPerTopic);

        return result;
    }

    public List<TestAttempt> hydrateAttemptReviews(List<TestAttempt> attempts) {
        Map<Integer, Question> questionsById = questions.stream()
                .collect(Collectors.toMap(question -> question.id, question -> question, (left, right) -> left));

        for (TestAttempt attempt : attempts) {
            if (attempt.getQuestionReviews() == null) {
                continue;
            }

            for (AttemptQuestionReview review : attempt.getQuestionReviews()) {
                Question question = questionsById.get(review.getQuestionId());
                if (question == null) {
                    continue;
                }

                review.setQuestionText(question.question);
                review.setSubject(question.subject);
                review.setTopic(question.topic);
                review.setSelectedAnswer(getOptionText(question, review.getSelectedOption()));
                review.setCorrectAnswer(getOptionText(question, review.getCorrectOption()));
            }
        }

        return attempts;
    }

    private AttemptQuestionReview toQuestionReview(Question question, Integer selectedOption, boolean correct) {
        AttemptQuestionReview review = new AttemptQuestionReview();
        review.setQuestionId(question.id);
        review.setSubject(question.subject);
        review.setTopic(question.topic);
        review.setSelectedOption(selectedOption);
        review.setCorrectOption(question.answer);
        review.setCorrect(correct);
        return review;
    }

    private String getOptionText(Question question, Integer optionIndex) {
        if (optionIndex == null) {
            return "Not answered";
        }

        if (optionIndex < 0 || optionIndex >= question.options.size()) {
            return "Invalid option";
        }

        return question.options.get(optionIndex);
    }

    private String buildAttemptKey(String userId, String subject) {
        return userId + "::" + subject.trim().toUpperCase();
    }

    private record SelectionHistory(
            Map<Integer, Integer> seenCountByQuestionId,
            Map<Integer, Integer> lastSeenRankByQuestionId,
            Set<Integer> recentQuestionIds
    ) {
    }
}
