package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.Annotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnotationRepository extends JpaRepository<Annotation, UUID> {
    List<Annotation> findByFicheIdOrderByCreatedAtAsc(UUID ficheId);
    void deleteByFicheId(UUID ficheId);
}
