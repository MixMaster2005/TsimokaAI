package mg.esmia.miage.spaceservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
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
 * Génération et enrichissement du persona pédagogique par LLM (cf. CDC §4.1 et
 * ARCHITECTURE.md §6.3).
 *
 * <ul>
 *   <li>{@code generateInitialPersona} : appel LLM one-shot (provider actif via
 *       {@link ChatProviderResolver}, cf. {@code ACTIVE_LLM_PROVIDER}) à partir de
 *       nom/tag/description de l'espace, prompt {@code prompts/persona-generation.st}.
 *       Le résultat devient l'instruction système injectée par chat-service.</li>
 *   <li>{@code enrichPersonaAfterIngestion} : déclenché par {@code IngestionEventListener}
 *       sur DOCUMENT_READY. Un échantillon des chunks du document est récupéré depuis Qdrant
 *       (collection UNIQUE {@code chunks}, payload {@code space_id}/{@code document_id},
 *       filtre {@code space_id == 'x' and document_id == 'y'}) puis fusionné dans le persona
 *       existant via {@code prompts/persona-enrichment.st}.</li>
 * </ul>
 *
 * <p>Chaîne de repli : si l'appel LLM échoue ou que le circuit {@code llm-persona} est ouvert,
 * on retombe sur un persona « template » déterministe (génération) ou sur le persona inchangé
 * (enrichissement) — le service ne bloque jamais le flux de création d'espace ni l'ingestion.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonaService {

    private static final String GENERIC_PERSONA_TEMPLATE = """
            Tu es un assistant pédagogique spécialisé en %s. Adopte un registre disciplinaire \
            rigoureux, structure tes réponses avec des définitions précises et des exemples \
            concrets. (Persona générique — la génération LLM est indisponible.)
            """;

    private static final int MAX_CHUNK_CHARS = 800;

    private final ChatProviderResolver chatProviderResolver;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/persona-generation.st")
    private Resource personaGenerationPrompt;

    @Value("classpath:prompts/persona-enrichment.st")
    private Resource personaEnrichmentPrompt;

    @Value("${persona.sample-size:8}")
    private int sampleSize;

    @CircuitBreaker(name = "llm-persona", fallbackMethod = "fallbackGenerate")
    public String generateInitialPersona(String spaceName, String subjectTag, String description) {
        String subject = (subjectTag == null || subjectTag.isBlank()) ? spaceName : subjectTag;
        String prompt = readPrompt(personaGenerationPrompt)
                .replace("{{SUBJECT}}", subject == null ? "" : subject)
                .replace("{{DESCRIPTION}}", description == null ? "" : description);
        String persona = chatProviderResolver.current().prompt()
                .system("Tu génères l'instruction système d'un assistant pédagogique. Réponds en français.")
                .user(prompt)
                .call()
                .content();
        return (persona == null || persona.isBlank())
                ? GENERIC_PERSONA_TEMPLATE.formatted(subject)
                : persona.strip();
    }

    private String fallbackGenerate(String spaceName, String subjectTag, String description, Throwable t) {
        log.warn("Fallback llm-persona (generateInitialPersona) : {}", t.getMessage());
        String subject = (subjectTag == null || subjectTag.isBlank()) ? spaceName : subjectTag;
        return GENERIC_PERSONA_TEMPLATE.formatted(subject);
    }

    @CircuitBreaker(name = "llm-persona", fallbackMethod = "fallbackEnrich")
    public String enrichPersonaAfterIngestion(String currentPersona, UUID spaceId, String documentId, int chunkCount) {
        List<Document> sample = sampleChunks(spaceId, documentId, chunkCount);
        if (sample.isEmpty()) {
            log.warn("Aucun chunk retrievé pour le document {} de l'espace {}, persona inchangé", documentId, spaceId);
            return currentPersona;
        }
        String chunks = sample.stream()
                .map(d -> d.getText() == null ? "" : d.getText())
                .map(text -> text.length() > MAX_CHUNK_CHARS ? text.substring(0, MAX_CHUNK_CHARS) : text)
                .collect(Collectors.joining("\n\n"));
        String prompt = readPrompt(personaEnrichmentPrompt)
                .replace("{{CURRENT_PERSONA}}", currentPersona == null ? "" : currentPersona)
                .replace("{{CHUNKS}}", chunks);
        String enriched = chatProviderResolver.current().prompt()
                .system("Tu enrichis l'instruction système d'un assistant pédagogique. "
                        + "Réponds uniquement avec la nouvelle instruction système complète, en français.")
                .user(prompt)
                .call()
                .content();
        return (enriched == null || enriched.isBlank()) ? currentPersona : enriched.strip();
    }

    private String fallbackEnrich(String currentPersona, UUID spaceId, String documentId, int chunkCount, Throwable t) {
        log.warn("Fallback llm-persona (enrichPersonaAfterIngestion, document={}) : {}", documentId, t.getMessage());
        return currentPersona;
    }

    /**
     * Échantillon des chunks du document depuis Qdrant : filtre sur {@code space_id} et
     * {@code document_id} (payload), requête neutre + seuil 0 pour récupérer le contenu
     * sans dépendre de la pertinence sémantique (on veut un échantillon représentatif,
     * pas les chunks « les plus proches » d'une question).
     */
    private List<Document> sampleChunks(UUID spaceId, String documentId, int chunkCount) {
        int topK = Math.max(1, Math.min(chunkCount <= 0 ? sampleSize : chunkCount, sampleSize));
        try {
            return vectorStore.similaritySearch(SearchRequest.builder()
                    .query("extrait du contenu du document")
                    .topK(topK)
                    .similarityThreshold(0.0)
                    .filterExpression("space_id == '" + spaceId + "' and document_id == '" + documentId + "'")
                    .build());
        } catch (Exception e) {
            log.warn("Échantillon de chunks indisponible (space={}, doc={}) : {}", spaceId, documentId, e.getMessage());
            return List.of();
        }
    }

    private String readPrompt(Resource resource) {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt illisible : " + resource.getFilename(), e);
        }
    }
}