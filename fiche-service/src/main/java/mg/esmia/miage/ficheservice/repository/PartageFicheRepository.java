package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.PartageFiche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartageFicheRepository extends JpaRepository<PartageFiche, UUID> {
    List<PartageFiche> findByFicheId(UUID ficheId);
    List<PartageFiche> findByDestinataireId(UUID destinataireId);
    void deleteByFicheId(UUID ficheId);
}
