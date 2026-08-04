package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.ingestionservice.dto.DocumentResponse;
import mg.esmia.miage.ingestionservice.entity.Document;
import mg.esmia.miage.ingestionservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final IngestionPipelineService pipelineService;

    @Transactional
    public DocumentResponse upload(UUID spaceId, UUID userId, MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new mg.esmia.miage.common.exception.BadRequestException(
                    "Format non supporté (PDF, DOCX, TXT uniquement) : " + mimeType);
        }

        String storageUrl = minioService.upload(file, spaceId);

        Document document = Document.builder()
                .spaceId(spaceId)
                .userId(userId)
                .filename(file.getOriginalFilename())
                .mimeType(mimeType)
                .storageUrl(storageUrl)
                .status(Document.Status.PENDING)
                .chunkCount(0)
                .build();
        document = documentRepository.save(document);

        pipelineService.processAsync(document.getId());

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
        pipelineService.deleteDocument(document);
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        documentRepository.findBySpaceId(spaceId).forEach(pipelineService::deleteDocument);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        documentRepository.findByUserId(userId).forEach(pipelineService::deleteDocument);
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
}
