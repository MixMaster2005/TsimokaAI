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

    /**
     * Formats acceptés — alignés sur les convertisseurs locaux de MarkItDown
     * (vérifié sur markitdown 0.1.7) et sur le découpage par extension de docling-worker.
     * Format exclu volontairement : images/audio (nécessitent un LLM de description),
     * zip/ipynb/msg (non documents de cours).
     */
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            // PDF
            "application/pdf", "application/x-pdf",
            // Traitement de texte
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Présentations
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // Tableurs
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel", "application/excel",
            // Données tabulaires / texte
            "text/csv", "application/csv",
            "text/plain", "text/markdown", "text/x-markdown",
            // Web / livres numériques
            "text/html", "application/xhtml+xml",
            "application/epub+zip", "application/epub");

    /**
     * Repli par extension : certains clients envoient un MIME imprécis voire nul pour
     * EPUB/Markdown/CSV (ex. {@code application/octet-stream}) — docling-worker routant
     * sur l'extension, on accepte tant que l'un des deux est dans la liste.
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "md", "markdown",
            "pptx", "xlsx", "xls", "csv", "html", "htm", "epub");

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final IngestionPipelineService pipelineService;

    @Transactional
    public DocumentResponse upload(UUID spaceId, UUID userId, MultipartFile file) {
        boolean mimeOk = file.getContentType() != null && SUPPORTED_MIME_TYPES.contains(file.getContentType());
        boolean extensionOk = SUPPORTED_EXTENSIONS.contains(extensionOf(file.getOriginalFilename()));
        if (!mimeOk && !extensionOk) {
            throw new mg.esmia.miage.common.exception.BadRequestException(
                    "Format non supporté (PDF, DOCX, TXT, Markdown, PPTX, XLSX, XLS, CSV, HTML, EPUB) : "
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

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    private void assertOwnerOrAdmin(Document document, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !document.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à ce document");
        }
    }
}
