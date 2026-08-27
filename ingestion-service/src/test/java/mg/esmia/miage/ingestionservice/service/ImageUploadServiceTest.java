package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult.DoclingImage;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUploadServiceTest {

    private final FakeMinioService minioService = new FakeMinioService();
    private final ImageUploadService service = new ImageUploadService(minioService);

    @Test
    void substitueCaptionRenseigneeAvecDescription() {
        UUID spaceId = UUID.randomUUID();
        minioService.storageUrl = "documents/spaces/images/img_001.png";

        String result = service.substituteImages("Avant\n\n{{IMAGE:img_001}}\n\nApres", List.of(
                image("img_001", "Schema de migration")
        ), spaceId);

        assertEquals("""
                Avant

                ![Schema de migration](documents/spaces/images/img_001.png)
                > **Description :** Schema de migration

                Apres""", result);
    }

    @Test
    void substitueCaptionVideSansDescriptionVide() {
        UUID spaceId = UUID.randomUUID();
        minioService.storageUrl = "documents/spaces/images/img_001.png";

        String result = service.substituteImages("{{IMAGE:img_001}}", List.of(
                image("img_001", "  ")
        ), spaceId);

        assertEquals("![Figure extraite du document](documents/spaces/images/img_001.png)", result);
    }

    private static DoclingImage image(String placeholderId, String caption) {
        return new DoclingImage(
                placeholderId,
                "image/png",
                Base64.getEncoder().encodeToString("image".getBytes()),
                caption
        );
    }

    private static class FakeMinioService extends MinioService {
        private String storageUrl;

        FakeMinioService() {
            super(null);
        }

        @Override
        public String uploadBytes(byte[] content, String contentType, UUID spaceId, String suggestedFilename) {
            return storageUrl;
        }
    }
}
