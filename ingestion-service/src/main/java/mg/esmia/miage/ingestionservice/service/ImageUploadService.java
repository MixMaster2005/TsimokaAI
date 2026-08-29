package mg.esmia.miage.ingestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.ingestionservice.entity.DocumentImage;
import mg.esmia.miage.ingestionservice.repository.DocumentImageRepository;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult.DoclingImage;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Traitement des figures extraites par docling-worker (spec v2) : upload dans MinIO du binaire
 * (décodé du base64 renvoyé par le worker) puis substitution de chaque placeholder
 * {@code {{IMAGE:img_001}}} du Markdown par {@code ![caption](url)} +
 * {@code > **Description :** caption}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final MinioService minioService;
    private final DocumentImageRepository documentImageRepository;

    /**
     * @return le Markdown avec les images uplodées dans MinIO et les placeholders substitués.
     */
    public String substituteImages(String markdown, List<DoclingImage> images, UUID spaceId, UUID documentId) {
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
            documentImageRepository.save(DocumentImage.builder()
                    .documentId(documentId)
                    .storageUrl(storageUrl)
                    .build());
            String caption = image.caption() == null ? "" : image.caption().trim();
            String altText = caption.isBlank() ? "Figure extraite du document" : caption;
            String replacement = caption.isBlank()
                    ? "![%s](%s)".formatted(altText, storageUrl)
                    : "![%s](%s)%n> **Description :** %s".formatted(altText, storageUrl, caption);
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
