package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.CanonicalDocument;
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
 * Orchestrateur du pipeline d'ingestion : extraction → chunking → embedding → indexation
 * vectorielle.
 *
 * <p>Étapes :
 * <ol>
 *   <li>Télécharger le fichier depuis MinIO.</li>
 *   <li>Extraire via docling-worker (conteneur Python spawné à la demande) → AST +
 *       Markdown + images.</li>
 *   <li>Upload des images dans MinIO + substitution des placeholders dans le Markdown.</li>
 *   <li>Découper en chunks structurés :
 *       <ul>
 *         <li>PDF (AST disponible) → {@link StructureAwareChunker} ;</li>
 *         <li>Non-PDF (pas d'AST) → {@link MarkdownFallbackChunker} (fallback Markdown).</li>
 *       </ul>
 *   </li>
 *   <li>Générer les embeddings pour chaque chunk.</li>
 *   <li>Upsert dans Qdrant (multi-tenant, métadonnées enrichies).</li>
 *   <li>Persister les entités Chunk en base.</li>
 *   <li>Mettre à jour le statut du document.</li>
 * </ol>
 *
 * <p>Le Markdown est conservé pour le debug/preview mais ne participe plus au chunking.</p>
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
    private final StructureAwareChunker astChunker;
    private final MarkdownFallbackChunker markdownFallbackChunker;
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

            // Notifie les clients SSE que le traitement a commencé
            eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                    IngestionEvent.processing(document.getId().toString(), document.getSpaceId().toString(),
                            document.getUserId().toString()));

            // Étape 1-2 : téléchargement MinIO + extraction via docling-worker
            byte[] fileContent;
            try (InputStream is = minioService.download(document.getStorageUrl())) {
                fileContent = is.readAllBytes();
            }
            DoclingConversionResult conversion = dockerWorkerClient.convert(fileContent, document.getFilename());

            // Étape 3 : chunking — AST si disponible (PDF), sinon fallback Markdown
            CanonicalDocument ast = conversion.document();
            List<StructuredChunk> structuredChunks;
            if (ast != null && ast.pages() != null && !ast.pages().isEmpty()) {
                structuredChunks = astChunker.chunk(ast);
            } else {
                structuredChunks = markdownFallbackChunker.chunk(conversion.markdown());
            }
            if (structuredChunks.isEmpty()) {
                throw new IllegalStateException("Aucun contenu textuel extrait du document");
            }

            // Markdown pour debug/preview (pas pour le chunking)
            String markdown = imageUploadService.substituteImages(
                    conversion.markdown(), conversion.images(), document.getSpaceId());
            log.info("Document {} converti : méthode {}, {} pages, {} chunks, {} caractères Markdown",
                    documentId, conversion.method(), conversion.pagesProcessed(),
                    structuredChunks.size(), markdown.length());

            // Étape 4 : embeddings (Spring AI / Ollama) — un vecteur par chunk
            List<String> chunkTexts = structuredChunks.stream()
                    .map(StructuredChunk::text).toList();
            List<float[]> vectors = embeddingModel.embed(chunkTexts);
            if (vectors.size() != structuredChunks.size()) {
                throw new IllegalStateException(
                        "Nombre d'embeddings incohérent (%d embeddings pour %d chunks)".formatted(
                                vectors.size(), structuredChunks.size()));
            }

            // Étape 5-6 : upsert Qdrant (métadonnées enrichies) + persistance Chunk
            qdrantVectorService.ensureCollection(vectors.get(0).length);
            List<UUID> vectorIds = qdrantVectorService.upsertChunks(
                    document.getId(), document.getSpaceId(), structuredChunks, vectors);
            saveChunks(document.getId(), structuredChunks, vectorIds);

            // Étape 7 : statut READY + DOCUMENT_READY
            document.setStatus(Document.Status.READY);
            document.setChunkCount(structuredChunks.size());
            documentRepository.save(document);

            eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                    IngestionEvent.ready(document.getId().toString(), document.getSpaceId().toString(),
                            document.getUserId().toString(), structuredChunks.size()));

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

    /** Persiste une entité {@link Chunk} par morceau (vectorId = point Qdrant). */
    private void saveChunks(UUID documentId, List<StructuredChunk> chunks, List<UUID> vectorIds) {
        List<Chunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            entities.add(Chunk.builder()
                    .documentId(documentId)
                    .chunkIndex(chunks.get(i).chunkIndex())
                    .content(chunks.get(i).text())
                    .tokenCount(0) // temporaire — calculé ailleurs en V2
                    .vectorId(vectorIds.get(i).toString())
                    .build());
        }
        chunkRepository.saveAll(entities);
    }
}
