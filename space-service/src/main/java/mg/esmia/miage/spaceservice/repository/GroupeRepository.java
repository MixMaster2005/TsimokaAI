package mg.esmia.miage.spaceservice.repository;

import mg.esmia.miage.spaceservice.entity.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupeRepository extends JpaRepository<Groupe, UUID> {
    List<Groupe> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
}
