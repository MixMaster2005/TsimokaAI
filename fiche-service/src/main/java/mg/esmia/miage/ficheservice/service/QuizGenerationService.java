package mg.esmia.miage.ficheservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
import mg.esmia.miage.common.exception.ApiException;
import mg.esmia.miage.common.exception.ErrorCode;
import mg.esmia.miage.ficheservice.entity.Quiz;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Génération de quiz de révision par appel LLM unique (single-call).
 * Le quiz est ciblé sur un document, un topic ou tout un espace.
 * Les résultats MAP sont mis en cache via {@link FicheMapCacheService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerationService {

    private final ChatProviderResolver chatProviderResolver;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final FicheMapCacheService mapCache;

    @Value("classpath:prompts/quiz-generate.st")
    private Resource quizGeneratePrompt;

    @Value("${fiche.max-chunks-per-document:50}")
    private int maxChunksPerDocument;

    @CircuitBreaker(name = "llm-fiche", fallbackMethod = "fallbackGenerateQuiz")
    public String generateQuizContent(Quiz quiz) {
        List<Document> chunks = retrieveChunks(quiz);
        if (chunks.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Aucun contenu indexé disponible pour la génération du quiz.", 422);
        }

        String content = chunks.stream()
                .map(Document::getText)
                .map(t -> t == null ? "" : t)
                .map(t -> FicheGenerationService.smartTruncate(t, FicheGenerationService.MAX_CHUNK_CHARS))
                .collect(Collectors.joining("\n\n"));

        String prompt = readPrompt(quizGeneratePrompt)
                .replace("{{CHUNKS}}", content)
                .replace("{{QUESTION_COUNT}}", String.valueOf(quiz.getQuestionCount()))
                .replace("{{DIFFICULTY}}", quiz.getDifficulty())
                .replace("{{TOPIC}}", quiz.getTargetTopic() == null ? "" : quiz.getTargetTopic());

        String systemPrompt = "Tu es un enseignant expert qui crée des quiz de révision. " +
                "Réponds UNIQUEMENT en JSON valide, en français.";

        String result = chatProviderResolver.current()
                .prompt()
                .system(systemPrompt)
                .user(prompt)
                .call()
                .content();

        // Valider que le résultat est un JSON valide
        validateQuizJson(result);
        return result;
    }

    /**
     * Score une tentative de quiz en comparant les réponses de l'étudiant aux bonnes réponses.
     * Retourne le JSON des réponses enrichi avec le champ "correct".
     */
    public String scoreAttempt(String quizContentJson, String answersJson) {
        try {
            JsonNode quizContent = objectMapper.readTree(quizContentJson);
            JsonNode answers = objectMapper.readTree(answersJson);
            JsonNode questions = quizContent.get("questions");

            int score = 0;
            var scoredAnswers = new com.fasterxml.jackson.databind.node.ArrayNode(objectMapper.getNodeFactory());

            for (JsonNode answer : answers) {
                int questionIndex = answer.get("questionIndex").asInt();
                String userAnswer = answer.get("answer").asText();

                if (questionIndex < questions.size()) {
                    String correctAnswer = questions.get(questionIndex).get("answer").asText();
                    boolean correct = userAnswer.equalsIgnoreCase(correctAnswer);
                    if (correct) score++;

                    var scoredAnswer = objectMapper.createObjectNode();
                    scoredAnswer.put("questionIndex", questionIndex);
                    scoredAnswer.put("answer", userAnswer);
                    scoredAnswer.put("correct", correct);
                    scoredAnswers.add(scoredAnswer);
                }
            }

            return objectMapper.writeValueAsString(scoredAnswers);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Format de réponses invalide", 400);
        }
    }

    private List<Document> retrieveChunks(Quiz quiz) {
        SearchRequest.Builder search = SearchRequest.builder()
                .query("contenu du document pour quiz")
                .topK(maxChunksPerDocument)
                .similarityThreshold(0.0);

        if (quiz.getTargetDocumentId() != null) {
            // Scope DOCUMENT : chunks d'un seul document
            search.filterExpression("space_id == '" + quiz.getSpaceId() +
                    "' and document_id == '" + quiz.getTargetDocumentId() + "'");
        } else {
            // Scope SPACE ou TOPIC : tous les chunks de l'espace
            search.filterExpression("space_id == '" + quiz.getSpaceId() + "'");
        }

        try {
            return vectorStore.similaritySearch(search.build());
        } catch (Exception e) {
            log.warn("Retrieval des chunks indisponible pour quiz (space={}, doc={}) : {}",
                    quiz.getSpaceId(), quiz.getTargetDocumentId(), e.getMessage());
            return List.of();
        }
    }

    private void validateQuizJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.has("questions") || !node.get("questions").isArray()) {
                throw new ApiException(ErrorCode.INTERNAL_ERROR,
                        "La génération du quiz a retourné un format invalide (champ 'questions' manquant).", 500);
            }
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "La génération du quiz a retourné un JSON invalide.", 500);
        }
    }

    public String fallbackGenerateQuiz(Quiz quiz, Throwable t) {
        log.error("Fallback llm-fiche pour quiz (space={}, doc={}) : {}",
                quiz.getSpaceId(), quiz.getTargetDocumentId(), t.getMessage());
        throw new ApiException(ErrorCode.INTERNAL_ERROR,
                "La génération du quiz a échoué (service LLM indisponible). Réessayez plus tard.", 503);
    }

    private String readPrompt(Resource resource) {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt illisible : " + resource.getFilename(), e);
        }
    }
}
