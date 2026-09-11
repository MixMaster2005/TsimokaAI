package mg.esmia.miage.ficheservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mg.esmia.miage.ficheservice.entity.Quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue publique du quiz pour l'étudiant : questions + options SANS {@code answer} ni {@code explanation}.
 * Le filtrage est appliqué sur {@code contentJson} via ObjectMapper :
 * {@code contentJson.questions[]} ne garde que {@code type/question/options}.
 */
public record QuizPublicResponse(
        UUID id, UUID spaceId, UUID userId, String title,
        String scope, UUID targetDocumentId, String targetTopic,
        UUID sourceFicheId, List<UUID> sourceDocumentIds,
        String difficulty, int questionCount,
        @JsonProperty("contentJson") String contentJsonSansReponses,
        String statut,
        boolean obsolete, Instant generatedAt, Instant updatedAt
) implements QuizView {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static QuizPublicResponse from(Quiz q) {
        return new QuizPublicResponse(
                q.getId(), q.getSpaceId(), q.getUserId(), q.getTitle(),
                q.getScope(), q.getTargetDocumentId(), q.getTargetTopic(),
                q.getSourceFicheId(),
                q.getSourceDocumentIds() == null ? List.of() : List.of(q.getSourceDocumentIds()),
                q.getDifficulty(), q.getQuestionCount(),
                sanitizeContent(q.getContentJson()),
                q.getStatut(),
                q.isObsolete(), q.getGeneratedAt(), q.getUpdatedAt()
        );
    }

    /**
     * Filtre le contentJson brut : ne garde que type/question/options par question.
     * Préserve les autres champs top-level. Ne fait jamais fuiter answer/explanation.
     */
    public static String sanitizeContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return contentJson;
        }
        try {
            JsonNode root = MAPPER.readTree(contentJson);
            if (root.isArray()) {
                ArrayNode filtered = MAPPER.createArrayNode();
                for (JsonNode q : root) {
                    filtered.add(filterQuestion(q));
                }
                return MAPPER.writeValueAsString(filtered);
            }
            if (!root.isObject()) {
                return contentJson;
            }
            ObjectNode copy = ((ObjectNode) root).deepCopy();
            // Supprime toute fuite générique answer/explanation au top-level
            copy.remove("answer");
            copy.remove("explanation");
            JsonNode questions = copy.get("questions");
            if (questions != null && questions.isArray()) {
                ArrayNode filtered = MAPPER.createArrayNode();
                for (JsonNode q : questions) {
                    filtered.add(filterQuestion(q));
                }
                copy.set("questions", filtered);
            } else {
                // Forme inconnue : strip récursif par sécurité
                stripSensitive(copy);
            }
            return MAPPER.writeValueAsString(copy);
        } catch (Exception e) {
            // En cas de JSON illisible, ne pas fuiter le brut : retourner une enveloppe vide
            return "{\"questions\":[]}";
        }
    }

    private static ObjectNode filterQuestion(JsonNode q) {
        ObjectNode out = MAPPER.createObjectNode();
        if (q != null && q.isObject()) {
            if (q.has("type")) {
                out.set("type", q.get("type"));
            }
            if (q.has("question")) {
                out.set("question", q.get("question"));
            }
            if (q.has("options")) {
                out.set("options", q.get("options"));
            }
        }
        return out;
    }

    private static void stripSensitive(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            obj.remove("answer");
            obj.remove("explanation");
            var fields = obj.fields();
            while (fields.hasNext()) {
                stripSensitive(fields.next().getValue());
            }
        } else if (node instanceof ArrayNode arr) {
            for (JsonNode child : arr) {
                stripSensitive(child);
            }
        }
    }
}
