package mg.esmia.miage.ingestionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.IngestionEvent;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ingestionservice.config.SseEmittersRegistry;
import mg.esmia.miage.ingestionservice.dto.DocumentResponse;
import mg.esmia.miage.ingestionservice.model.SupportedDocumentType;
import mg.esmia.miage.ingestionservice.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final SseEmittersRegistry sseRegistry;
    private final RedisEventPublisher eventPublisher;

    /** Taille maximale d'un fichier uploadé : 20 Mo. */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    /**
     * Point d'entrée SSE : le client ouvre une connexion GET et reçoit en temps réel
     * les changements de statut des documents de l'espace.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        log.debug("Ouverture SSE pour space {} par user {}", spaceId, ctx.userId());

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout
        sseRegistry.register(spaceId, emitter);
        return emitter;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@RequestParam UUID spaceId, @RequestParam("file") MultipartFile file) {
        UserContext ctx = authenticated();
        validateFile(file);
        DocumentResponse doc = documentService.upload(spaceId, UUID.fromString(ctx.userId()), file);

        // Push immédiat du statut PENDING via Redis → SSE
        eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                IngestionEvent.processing(doc.id().toString(), spaceId.toString(), ctx.userId()));

        return ApiResponse.success(doc, ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> listBySpace(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(documentService.listBySpace(spaceId), ctx.requestId());
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                documentService.getById(id, UUID.fromString(ctx.userId()), ctx.isAdmin()), ctx.requestId());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        documentService.delete(id, UUID.fromString(ctx.userId()), ctx.isAdmin());
        return ApiResponse.success(null, ctx.requestId());
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<DocumentResponse> retry(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        DocumentResponse doc = documentService.retry(id, UUID.fromString(ctx.userId()), ctx.isAdmin());

        // Push immédiat du statut PENDING via Redis → SSE
        eventPublisher.publish(EventChannels.INGESTION_EVENTS,
                IngestionEvent.processing(doc.id().toString(), doc.spaceId().toString(), ctx.userId()));

        return ApiResponse.success(doc, ctx.requestId());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Le fichier dépasse la taille maximale de 20 Mo");
        }
        String contentType = file.getContentType();
        String extension = SupportedDocumentType.canonicalExtension(file.getOriginalFilename());
        if (SupportedDocumentType.fromMimeType(contentType) == null && extension == null) {
            throw new BadRequestException("Type de fichier non supporté : " + contentType
                    + ". " + SupportedDocumentType.supportedFormatsMessage());
        }
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
