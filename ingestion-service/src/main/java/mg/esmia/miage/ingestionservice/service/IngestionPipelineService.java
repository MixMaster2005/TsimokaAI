package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ingestionservice.entity.Chunk;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.repository.ChunkRepository;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult;
import mg.esmia.miage.ingestionservice.service.docker.DockerWorkerClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrateur du pipeline d'ingestion : extraction -> chunking -> embedding -> indexation
 * vectorielle. Chaque étape est déléguée à un service dédié (injection par constructeur) :
 * <ul>
 *   <li>MinIO (téléchargement/suppression du binaire) — {@link MinioService} ;</li>
 *   <li>extraction via docling-worker (conteneur spawné à la demande) — {@link DockerWorkerClient} ;</li>
 *   <li>upload des figures + substitution des placeholders — {@link ImageUploadService} ;</li>
 *   <li>découpage orienté sens (titres {@code #}/{@code ##}, récursif si trop grand) — {@link MarkdownChunkingService} ;</li>
 *   <li>embeddings (un vecteur par chunk, en lot) — {@code EmbeddingModel} Spring AI / Ollama ;</li>
 *   <li>collection + upsert + purge Qdrant — {@link QdrantVectorService}.</li>
 * </ul>
 *
 * <p>Etapes (cf. CDC §4.2) :
 * <ol>
 *   <li>Télécharger le fichier depuis MinIO.</li>
 *   <li>Extraire le texte via docling-worker (conteneur Python spawné à la demande) ; les
 *       images extraites sont uplodées dans MinIO et les placeholders substitués (spec v2).</li>
 *   <li>Découper le Markdown en chunks orientés sens (~500 tokens / chunk) — titres d'abord,
 *       découpe de secours seulement si une section est trop grande.</li>
 *   <li>Générer les embeddings (EmbeddingModel Spring AI / Ollama) pour chaque chunk.</li>
 *   <li>Upsert des points dans la collection Qdrant unique "chunks" (multi-tenant, Option A) :
 *       chaque point porte document_id, space_id, chunk_index, content en payload — le
 *       cloisonnement par espace se fait par filtre au moment du retrieval.</li>
 *   <li>Persister les entités Chunk (vectorId = id du point Qdrant).</li>
 *   <li>Mettre à jour Document (status=READY, chunkCount) et publier DOCUMENT_READY.
 *       En cas d'échec à n'importe quelle étape : status=FAILED + publier DOCUMENT_FAILED.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionPipelineService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final MinioService minioService;
    private final RedisEventPublisher eventPublisher;
    private final DockerWorkerClient dockerWorkerClient;
    private final EmbeddingModel embeddingModel;
    private final ImageUploadService imageUploadService;
    private final MarkdownChunkingService chunkingService;
    private final QdrantVectorService qdrantVectorService;

    @Async("ingestionExecutor")
    @Transactional
    public void processAsync(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("processAsync appelé pour un document introuvable : {}", documentId);
            return;
        }

        try {
            document.setStatus(Document.Status.PROCESSING);
            documentRepository.save(document);

            // Étape 1-2 : téléchargement MinIO + extraction via docling-worker (conteneur
            // spawné à la demande) + upload des images extraites / substitution des placeholders.
            byte[] fileContent;
            try (InputStream is = minioService.download(document.getStorageUrl())) {
                fileContent = is.readAllBytes();
            }
            DoclingConversionResult conversion = dockerWorkerClient.convert(fileContent, document.getFilename());
            String markdown = imageUploadService.substituteImages(
                    conversion.markdown(), conversion.images(), document.getSpaceId());

            log.info("Document {} converti : méthode {}, {} pages, {} caractères de Markdown, {} images, warnings={}",
                    documentId, conversion.method(), conversion.pagesProcessed(), markdown.length(),
                    conversion.images().size(), conversion.warnings());

            // Étape 3 : chunking orienté sens (titres # / ##, récursif si section trop grande).
            List<String> chunks = chunkingService.chunk(markdown);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("Aucun contenu textuel extrait du document");
            }

            // Étape 4 : embeddings (Spring AI / Ollama) — un vecteur par chunk.
            List<float[]> vectors = embeddingModel.embed(chunks);
            if (vectors.size() != chunks.size()) {
                throw new IllegalStateException(
                        "Nombre d'embeddings incohérent (%d embeddings pour %d chunks)".formatted(
                                vectors.size(), chunks.size()));
            }

            // Étape 5-6 : upsert dans la collection unique (multi-tenant, space_id en payload)
            // + persistance Chunk.
            qdrantVectorService.ensureCollection(vectors.get(0).length);
            List<UUID> vectorIds = qdrantVectorService.upsertChunks(
                    document.getId(), document.getSpaceId(), chunks, vectors);
            saveChunks(document.getId(), chunks, vectorIds);

            // Étape 7 : statut READY avec le vrai nombre de chunks + DOCUMENT_READY.
            document.setStatus(Document.Status.READY);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                    IngestionEvent.ready(document.getId().toString(), document.getSpaceId().toString(),
                            document.getUserId().toString(), chunks.size()));

        } catch (Exception e) {
            log.error("Échec du traitement du document {}", documentId, e);
            document.setStatus(Document.Status.FAILED);
            document.setFailureReason(e.getMessage());
            documentRepository.save(document);
            eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                    IngestionEvent.failed(document.getId().toString(), document.getSpaceId().toString(),
                            document.getUserId().toString(), e.getMessage()));
        }
    }

    /** Nettoyage complet (BDD + MinIO + Qdrant) pour un document donné. */
    @Transactional
    public void deleteDocument(Document document) {
        chunkRepository.deleteByDocumentId(document.getId());
        minioService.delete(document.getStorageUrl());
        qdrantVectorService.deletePoints(document.getId());
        documentRepository.delete(document);
    }

    /** Persiste une entité {@link Chunk} par morceau (token_count estimé, vectorId = point Qdrant). */
    private void saveChunks(UUID documentId, List<String> chunks, List<UUID> vectorIds) {
        List<Chunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            entities.add(Chunk.builder()
                    .documentId(documentId)
                    .chunkIndex(i)
                    .content(chunks.get(i))
                    .tokenCount(chunkingService.estimateTokenCount(chunks.get(i)))
                    .vectorId(vectorIds.get(i).toString())
                    .build());
        }
        chunkRepository.saveAll(entities);
    }
}
