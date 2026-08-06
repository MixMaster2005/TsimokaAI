package mg.esmia.miage.ingestionservice.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.exception.ApiException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Wrapper générique autour du client MinIO : upload/download/delete d'objets.
 * Infrastructure générique -> implémentation COMPLETE fournie (pas de TODO ici).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService implements InitializingBean {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO '{}' créé", bucket);
            }
        } catch (Exception e) {
            log.warn("Impossible de vérifier/créer le bucket MinIO au démarrage : {}", e.getMessage());
        }
    }

    /**
     * @return l'URL logique de stockage (bucket/objectName), à conserver en base
     * (jamais le binaire lui-même, cf. contrat de données).
     */
    public String upload(MultipartFile file, UUID spaceId) {
        String objectName = "spaces/%s/%s-%s".formatted(spaceId, UUID.randomUUID(), file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return bucket + "/" + objectName;
        } catch (Exception e) {
            log.error("Échec de l'upload MinIO", e);
            throw new ApiException("STORAGE_ERROR", "Échec du stockage du fichier", 500);
        }
    }

    /**
     * Upload d'images extraites par docling-worker (spec v2). Préfixe
     * {@code spaces/{spaceId}/images/} pour distinguer les images du document original.
     *
     * @return l'URL logique de stockage (bucket/objectName), à insérer dans le Markdown.
     */
    public String uploadBytes(byte[] content, String contentType, UUID spaceId, String suggestedFilename) {
        String objectName = "spaces/%s/images/%s-%s".formatted(spaceId, UUID.randomUUID(), suggestedFilename);
        try (InputStream is = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, content.length, -1)
                    .contentType(contentType)
                    .build());
            return bucket + "/" + objectName;
        } catch (Exception e) {
            log.error("Échec de l'upload MinIO", e);
            throw new ApiException("STORAGE_ERROR", "Échec du stockage du fichier", 500);
        }
    }

    public InputStream download(String storageUrl) {
        String objectName = objectNameFrom(storageUrl);
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.error("Échec du téléchargement MinIO pour {}", storageUrl, e);
            throw new ApiException("STORAGE_ERROR", "Échec de la lecture du fichier", 500);
        }
    }

    public void delete(String storageUrl) {
        String objectName = objectNameFrom(storageUrl);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.warn("Échec de la suppression MinIO pour {} (ignoré)", storageUrl, e);
        }
    }

    private String objectNameFrom(String storageUrl) {
        // storageUrl == "bucket/spaces/.../fichier" -> on retire le préfixe "bucket/"
        return storageUrl.startsWith(bucket + "/") ? storageUrl.substring(bucket.length() + 1) : storageUrl;
    }
}
