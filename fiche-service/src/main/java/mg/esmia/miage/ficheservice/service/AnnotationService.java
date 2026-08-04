package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.ficheservice.dto.CreateAnnotationRequest;
import mg.esmia.miage.ficheservice.entity.Annotation;
import mg.esmia.miage.ficheservice.repository.AnnotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;

    @Transactional
    public Annotation add(UUID ficheId, UUID auteurId, CreateAnnotationRequest request) {
        Annotation annotation = Annotation.builder()
                .ficheId(ficheId)
                .auteurId(auteurId)
                .contenu(request.contenu())
                .sectionRef(request.sectionRef())
                .build();
        return annotationRepository.save(annotation);
    }

    public List<Annotation> listByFiche(UUID ficheId) {
        return annotationRepository.findByFicheIdOrderByCreatedAtAsc(ficheId);
    }
}
