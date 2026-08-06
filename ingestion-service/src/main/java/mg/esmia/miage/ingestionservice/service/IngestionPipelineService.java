package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.repository.ChunkRepository;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult.DoclingImage;
import mg.esmia.miage.ingestionservice.service.docker.DockerWorkerClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * ================================ TODO (cœur IA à implémenter) ================================
 * Pipeline d'ingestion : extraction -> chunking -> embedding -> indexation vectorielle.
 *
 * Etapes attendues (cf. CDC §4.2 et "Base de projet") :
 *   1. Télécharger le fichier depuis MinIO (MinioService.download, déjà prêt).
 *   2. Extraire le texte (DÉJÀ EN PLACE) : délégation à docling-worker, un conteneur Python
 *      FastAPI spawné à la demande (cf. service.docker.DockerWorkerClient) qui renvoie le
 *      document en Markdown structuré.
 *   3. Découper en chunks de taille fixe AVEC CHEVAUCHEMENT (chunking fixe + overlap,
 *      cf. stack technique). Un point de départ raisonnable : ~500 tokens / chunk,
 *      ~50-100 tokens de chevauchement — à ajuster empiriquement.
 *   4. Générer les embeddings (EmbeddingModel de Spring AI, cf. QdrantConfig pour le
 *      client vectoriel déjà configuré) pour chaque chunk.
 *   5. Upsert dans Qdrant, collection "chunks_{spaceId}" (la créer si absente, cf.
 *      contrat "Schémas BDD & contrats utilitaires" - metadata: document_id, space_id,
 *      chunk_index, content).
 *   6. Persister les entités Chunk (vectorId = id du point Qdrant).
 *   7. Mettre à jour Document (status=READY, chunkCount) et publier DOCUMENT_READY.
 *      En cas d'échec à n'importe quelle étape : status=FAILED + publier DOCUMENT_FAILED.
 *
 * L'implémentation ci-dessous fait transiter le document par les bons statuts et publie les
 * bons événements ; l'extraction du texte est fonctionnelle via docling-worker, mais le
 * chunking / embedding / upsert restent à écrire - à remplacer par la logique décrite ci-dessus.
 * ================================================================================================
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

    @Async
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

            // Étape 1 (implémentée) : téléchargement MinIO + extraction du texte via docling-worker
            // (conteneur spawné à la demande, cf. service.docker.DockerWorkerClient).
            byte[] fileContent;
            try (InputStream is = minioService.download(document.getStorageUrl())) {
                fileContent = is.readAllBytes();
            }
            DoclingConversionResult conversion = dockerWorkerClient.convert(fileContent, document.getFilename());
            String markdown = conversion.markdown();

            // Spec v2 : les images extraites par docling-worker (base64) sont uploadées
            // dans MinIO puis les placeholders {{IMAGE:img_001}} sont substitués par
            // `![caption](url)` + une ligne de description (légende Gemini).
            markdown = uploadExtractedImages(markdown, conversion.images(), document.getSpaceId());

            log.info("Document {} converti : méthode {}, {} pages, {} caractères de Markdown, {} images, warnings={}",
                    documentId, conversion.method(), conversion.pagesProcessed(), markdown.length(),
                    conversion.images().size(), conversion.warnings());

            // TODO : étapes 3 à 7 (chunking du Markdown avec chevauchement, embeddings Spring AI,
            // upsert Qdrant collection chunks_{spaceId}, persistance des Chunk, statut READY +
            // publication DOCUMENT_READY avec le vrai chunkCount).
            log.warn("IngestionPipelineService.processAsync : chunking/embeddings/upsert non implémentés (TODO) "
                    + "pour le document {}", documentId);
            int chunkCount = 0;

            document.setStatus(Document.Status.READY);
            document.setChunkCount(chunkCount);
            documentRepository.save(document);

            eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                    IngestionEvent.ready(document.getId().toString(), document.getSpaceId().toString(),
                            document.getUserId().toString(), chunkCount));

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
        // TODO : supprimer également les points Qdrant associés (filtrage par document_id
        // dans la collection chunks_{spaceId}) une fois le client Qdrant utilisé ci-dessus.
        documentRepository.delete(document);
    }

    /**
     * Uploade les images extraites par docling-worker dans MinIO et substitue chaque
     * placeholder {@code {{IMAGE:img_001}}} du Markdown par
     * {@code ![caption](url)} + {@code > **Description :** caption}.
     */
    private String uploadExtractedImages(String markdown, List<DoclingImage> images, UUID spaceId) {
        String result = markdown;
        for (DoclingImage image : images) {
            if (image.dataBase64() == null || image.placeholderId() == null) {
                log.warn("Image docling-worker invalide (placeholder/data manquants) — ignorée");
                continue;
            }
            String storageUrl;
            try {
                storageUrl = minioService.uploadBytes(
                        Base64.getDecoder().decode(image.dataBase64()),
                        image.contentType(),
                        spaceId,
                        image.placeholderId() + extensionFor(image.contentType()));
            } catch (IllegalArgumentException e) {
                log.warn("Base64 invalide pour l'image {} — ignorée ({})", image.placeholderId(), e.getMessage());
                continue;
            }
            String caption = image.caption() == null ? "" : image.caption();
            String replacement = "![%s](%s)%n> **Description :** %s".formatted(
                    caption, storageUrl, caption);
            result = result.replace("{{IMAGE:%s}}".formatted(image.placeholderId()), replacement);
        }
        return result;
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/tiff" -> ".tiff";
            case "image/bmp" -> ".bmp";
            default -> ".bin";
        };
    }
}
