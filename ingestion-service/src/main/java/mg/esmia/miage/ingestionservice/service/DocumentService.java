package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.ingestionservice.dto.DocumentResponse;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.model.SupportedDocumentType;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final IngestionPipelineService pipelineService;
    private final DocumentDeletionService documentDeletionService;

    @Transactional
    public DocumentResponse upload(UUID spaceId, UUID userId, MultipartFile file) {
        String extension = SupportedDocumentType.canonicalExtension(file.getOriginalFilename());
        if (SupportedDocumentType.fromMimeType(file.getContentType()) == null && extension == null) {
            throw new mg.esmia.miage.common.exception.BadRequestException(
                    SupportedDocumentType.supportedFormatsMessage() + " : "
                            + file.getContentType() + " / " + file.getOriginalFilename());
        }

        String storageUrl = minioService.upload(file, spaceId);

        Document document = Document.builder()
                .spaceId(spaceId)
                .userId(userId)
                .filename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .storageUrl(storageUrl)
                .status(Document.Status.PENDING)
                .chunkCount(0)
                .build();
        document = documentRepository.save(document);

        // Lancer le pipeline APRÈS le commit de la transaction :
        // le thread async doit pouvoir trouver le document en DB (isolation READ COMMITTED).
        final UUID savedId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pipelineService.processAsync(savedId);
            }
        });

        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> listBySpace(UUID spaceId) {
        return documentRepository.findBySpaceId(spaceId).stream().map(DocumentResponse::from).toList();
    }

    public DocumentResponse getById(UUID id, UUID requesterId, boolean isAdmin) {
        Document document = findOrThrow(id);
        assertOwnerOrAdmin(document, requesterId, isAdmin);
        return DocumentResponse.from(document);
    }

    @Transactional
    public void delete(UUID id, UUID requesterId, boolean isAdmin) {
        Document document = findOrThrow(id);
        assertOwnerOrAdmin(document, requesterId, isAdmin);
        documentDeletionService.deleteDocument(document);
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        documentDeletionService.deleteBySpace(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        documentDeletionService.deleteByUser(userId);
    }

    private Document findOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));
    }

    private void assertOwnerOrAdmin(Document document, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !document.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à ce document");
        }
    }

    public DocumentResponse retry(UUID id, UUID requesterId, boolean isAdmin) {
        Document document = findOrThrow(id);
        assertOwnerOrAdmin(document, requesterId, isAdmin);
        if (document.getStatus() != Document.Status.PENDING && document.getStatus() != Document.Status.FAILED) {
            return DocumentResponse.from(document); // rien à relancer
        }
        log.info("Relance manuelle du pipeline pour le document {}", id);
        pipelineService.processAsync(document.getId());
        return DocumentResponse.from(document);
    }
}
