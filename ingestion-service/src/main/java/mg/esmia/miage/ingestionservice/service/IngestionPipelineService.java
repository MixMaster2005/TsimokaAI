package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.repository.ChunkRepository;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ================================ TODO (cœur IA à implémenter) ================================
 * Pipeline d'ingestion : extraction -> chunking -> embedding -> indexation vectorielle.
 *
 * Etapes attendues (cf. CDC §4.2 et "Base de projet") :
 *   1. Télécharger le fichier depuis MinIO (MinioService.download, déjà prêt).
 *   2. Extraire le texte brut selon le mime-type (Apache Tika est déjà en dépendance :
 *      org.apache.tika.Tika().parseToString(inputStream)).
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
 * L'implémentation ci-dessous est un SQUELETTE FONCTIONNEL qui fait transiter le document
 * par les bons statuts et publie les bons événements, mais ne fait AUCUNE extraction/chunking/
 * embedding réels - à remplacer par la logique décrite ci-dessus.
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

            // TODO : remplacer ce bloc par le pipeline réel décrit ci-dessus.
            log.warn("IngestionPipelineService.processAsync : pipeline RAG non implémenté (TODO) " +
                    "pour le document {}", documentId);
            int chunkCount = 0; // TODO: nombre réel de chunks générés

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
}
