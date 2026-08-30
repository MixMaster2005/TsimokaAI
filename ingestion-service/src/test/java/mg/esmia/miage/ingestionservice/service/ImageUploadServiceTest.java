package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.entity.DocumentImage;
import mg.esmia.miage.ingestionservice.repository.DocumentImageRepository;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult.DoclingImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private DocumentImageRepository imageRepository;

    private ImageUploadService service;

    @Test
    void substitueCaptionRenseigneeAvecDescription() {
        service = new ImageUploadService(minioService, imageRepository);
        UUID spaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(minioService.uploadBytes(any(), any(), any(), any())).thenReturn("documents/spaces/images/img_001.png");

        String result = service.substituteImages("Avant\n\n{{IMAGE:img_001}}\n\nApres", List.of(
                image("img_001", "Schema de migration")
        ), spaceId, documentId);

        assertEquals("""
                Avant

                ![Schema de migration](documents/spaces/images/img_001.png)
                > **Description :** Schema de migration

                Apres""", result);

        ArgumentCaptor<DocumentImage> captor = ArgumentCaptor.forClass(DocumentImage.class);
        verify(imageRepository).save(captor.capture());
        DocumentImage saved = captor.getValue();
        assertEquals("img_001", saved.getPlaceholderId());
        assertEquals("Schema de migration", saved.getCaption());
        assertEquals(documentId, saved.getDocumentId());
    }

    @Test
    void substitueCaptionVideSansDescriptionVide() {
        service = new ImageUploadService(minioService, imageRepository);
        UUID spaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(minioService.uploadBytes(any(), any(), any(), any())).thenReturn("documents/spaces/images/img_001.png");

        String result = service.substituteImages("{{IMAGE:img_001}}", List.of(
                image("img_001", "  ")
        ), spaceId, documentId);

        assertEquals("![Figure extraite du document](documents/spaces/images/img_001.png)", result);

        ArgumentCaptor<DocumentImage> captor = ArgumentCaptor.forClass(DocumentImage.class);
        verify(imageRepository).save(captor.capture());
        DocumentImage saved = captor.getValue();
        assertEquals("img_001", saved.getPlaceholderId());
        assertNull(saved.getCaption());
    }

    private static DoclingImage image(String placeholderId, String caption) {
        return new DoclingImage(
                placeholderId,
                "image/png",
                Base64.getEncoder().encodeToString("image".getBytes()),
                caption
        );
    }
}
