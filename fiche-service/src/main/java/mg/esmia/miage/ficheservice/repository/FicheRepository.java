package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.Fiche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FicheRepository extends JpaRepository<Fiche, UUID> {
    List<Fiche> findBySpaceIdAndUserId(UUID spaceId, UUID userId);
    /** Toutes les fiches d'un utilisateur, tous espaces confondus (vue transverse "Mes fiches"). */
    List<Fiche> findByUserIdOrderByGeneratedAtDesc(UUID userId);
    List<Fiche> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
