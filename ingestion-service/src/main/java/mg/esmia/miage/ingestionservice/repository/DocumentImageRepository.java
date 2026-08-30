package mg.esmia.miage.ingestionservice.repository;

import mg.esmia.miage.ingestionservice.entity.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentImageRepository extends JpaRepository<DocumentImage, UUID> {
    List<DocumentImage> findByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);

    @Query("SELECT di FROM DocumentImage di WHERE di.placeholderId IN :ids")
    List<DocumentImage> findByPlaceholderIds(@Param("ids") Collection<String> placeholderIds);
}
