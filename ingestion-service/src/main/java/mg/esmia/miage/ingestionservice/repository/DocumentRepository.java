package mg.esmia.miage.ingestionservice.repository;

import mg.esmia.miage.ingestionservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findBySpaceId(UUID spaceId);
    List<Document> findByUserId(UUID userId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
