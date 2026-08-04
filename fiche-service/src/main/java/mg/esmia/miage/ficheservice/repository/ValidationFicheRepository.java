package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ValidationFicheRepository extends JpaRepository<ValidationFiche, UUID> {
    Optional<ValidationFiche> findByFicheId(UUID ficheId);
    void deleteByFicheId(UUID ficheId);
}
