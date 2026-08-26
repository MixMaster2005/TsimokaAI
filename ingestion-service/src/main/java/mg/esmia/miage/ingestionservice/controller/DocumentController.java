package mg.esmia.miage.ingestionservice.controller;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ingestionservice.dto.DocumentResponse;
import mg.esmia.miage.ingestionservice.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@RequestParam UUID spaceId, @RequestParam("file") MultipartFile file) {
        UserContext ctx = authenticated();
        return ApiResponse.success(documentService.upload(spaceId, UUID.fromString(ctx.userId()), file), ctx.requestId());
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
        return ApiResponse.success(
                documentService.retry(id, UUID.fromString(ctx.userId()), ctx.isAdmin()), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
