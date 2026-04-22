package com.exam.ccee.service;

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

    private final List<Question> questions;
    private final TestAttemptRepository testAttemptRepository;
    private final Map<String, List<Question>> activeTests = new ConcurrentHashMap<>();

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
        List<Question> generatedQuestions = generateTest(subject);
        activeTests.put(buildAttemptKey(userId, subject), generatedQuestions);
        return generatedQuestions;
    }

    private List<Question> generateTest(String subject) {

        Map<String, Integer> blueprint = getBlueprint(subject);

        List<Question> subjectQs = questions.stream()
                .filter(q -> q.subject.equalsIgnoreCase(subject))
                .toList();

        Map<String, List<Question>> byTopic = subjectQs.stream()
                .collect(Collectors.groupingBy(q -> q.topic.trim().toLowerCase()));

        List<Question> finalTest = new ArrayList<>();

        for (String topic : blueprint.keySet()) {

            List<Question> pool = byTopic.getOrDefault(topic.trim().toLowerCase(), new ArrayList<>());
            Collections.shuffle(pool, new Random());

            int required = blueprint.get(topic);
            int take = Math.min(required, pool.size());

            finalTest.addAll(pool.subList(0, take));
        }

        Collections.shuffle(finalTest);
        return finalTest;
    }

    private Map<String, Integer> getBlueprint(String subject) {

        return switch (subject.toUpperCase()) {
            case "CPP" -> getCPPBlueprint();
            case "DBMS" -> getDBMSBlueprint();
            case "JAVA" -> getJavaBlueprint();
            case "WEB" -> getWebBlueprint();
            default -> new HashMap<>();
        };
    }

    private Map<String, Integer> getCPPBlueprint() {
        Map<String, Integer> map = new HashMap<>();

        map.put("OOP Basics", 8); // classes, objects
        map.put("Constructors", 4);
        map.put("Inheritance & Polymorphism", 8);
        map.put("Operator Overloading", 4);
        map.put("Templates", 4);
        map.put("Exception Handling", 3);
        map.put("STL", 6);
        map.put("File Handling", 3);
        map.put("Dynamic Memory", 3);
        map.put("Misc", 2);

        return map;
    }

    private Map<String, Integer> getDBMSBlueprint() {
        Map<String, Integer> map = new HashMap<>();

        map.put("ER Modeling", 5);
        map.put("SQL", 10);
        map.put("Joins", 5);
        map.put("Indexes", 3);
        map.put("Transactions", 5);
        map.put("Normalization", 5);
        map.put("PLSQL", 5);
        map.put("Query Optimization", 3);
        map.put("NoSQL", 2);
        map.put("Misc", 2);

        return map;
    }

    private Map<String, Integer> getJavaBlueprint() {
        Map<String, Integer> map = new HashMap<>();

        map.put("Basics", 6);
        map.put("OOP", 8);
        map.put("Inheritance & Interfaces", 6);
        map.put("Exception Handling", 4);
        map.put("Collections", 6);
        map.put("Generics", 3);
        map.put("Multithreading", 5);
        map.put("File IO", 3);
        map.put("JDBC", 2);
        map.put("Misc", 2);

        return map;
    }

    private Map<String, Integer> getWebBlueprint() {
        Map<String, Integer> map = new HashMap<>();

        map.put("Servlets", 8);
        map.put("JSP", 6);
        map.put("JSTL", 3);
        map.put("MVC", 5);
        map.put("Sessions & Cookies", 5);
        map.put("Filters", 3);
        map.put("JDBC Integration", 4);
        map.put("Security", 3);
        map.put("REST API", 4);
        map.put("Misc", 4);

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
        Set<Integer> issuedQuestionIds = issuedQuestions.stream()
                .map(q -> q.id)
                .collect(Collectors.toSet());

        for (Question q : issuedQuestions) {

            if (!answers.containsKey(q.id)) continue;

            int selected = answers.get(q.id);

            if (selected == q.answer) {
                score++;
            } else {
                wrongPerTopic.put(q.topic,
                        wrongPerTopic.getOrDefault(q.topic, 0) + 1);
            }
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

        testAttemptRepository.save(attempt);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("total", total);
        result.put("wrongPerTopic", wrongPerTopic);

        return result;
    }

    private String buildAttemptKey(String userId, String subject) {
        return userId + "::" + subject.trim().toUpperCase();
    }
}
