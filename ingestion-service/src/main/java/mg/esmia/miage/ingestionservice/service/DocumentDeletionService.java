package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.repository.ChunkRepository;
import mg.esmia.miage.ingestionservice.repository.DocumentImageRepository;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Responsable du nettoyage complet d'un document agrégé logique :
 * Qdrant points + PostgreSQL chunks + MinIO images + PostgreSQL document.
 *
 * <p>Ordre de suppression : Qdrant → MinIO → PostgreSQL.
 * Les opérations externes (Qdrant, MinIO) sont fail-soft : en cas d'échec,
 * l'erreur est loggée et le nettoyage continue.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDeletionService {

    private final QdrantVectorService qdrantVectorService;
    private final MinioService minioService;
    private final ChunkRepository chunkRepository;
    private final DocumentImageRepository documentImageRepository;
    private final DocumentRepository documentRepository;

    /**
     * Nettoyage complet d'un document : Qdrant → MinIO → PostgreSQL.
     */
    public void deleteDocument(Document document) {
        UUID documentId = document.getId();
        UUID spaceId = document.getSpaceId();
        log.info("Début du nettoyage du document {} (espace {})", documentId, spaceId);

        // 1. Qdrant : supprimer les points vectoriels
        qdrantVectorService.deletePoints(documentId);

        // 2. MinIO : supprimer le fichier original
        minioService.delete(document.getStorageUrl());

        // 3. MinIO : supprimer les images extraites par prefix
        minioService.deleteByPrefix("spaces/%s/images/%s".formatted(spaceId, documentId));

        // 4. PostgreSQL : supprimer les chunks
        chunkRepository.deleteByDocumentId(documentId);

        // 5. PostgreSQL : supprimer les enregistrements d'images
        documentImageRepository.deleteByDocumentId(documentId);

        // 6. PostgreSQL : supprimer le document
        documentRepository.delete(document);

        log.info("Nettoyage du document {} terminé", documentId);
    }

    /**
     * Supprime tous les documents d'un espace.
     */
    public void deleteBySpace(UUID spaceId) {
        documentRepository.findBySpaceId(spaceId).forEach(this::deleteDocument);
    }

    /**
     * Supprime tous les documents d'un utilisateur.
     */
    public void deleteByUser(UUID userId) {
        documentRepository.findByUserId(userId).forEach(this::deleteDocument);
    }
}
