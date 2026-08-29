package mg.esmia.miage.ingestionservice.repository;

import mg.esmia.miage.ingestionservice.entity.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentImageRepository extends JpaRepository<DocumentImage, UUID> {
    List<DocumentImage> findByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}
