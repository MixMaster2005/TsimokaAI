package mg.esmia.miage.spaceservice.repository;

import mg.esmia.miage.spaceservice.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository extends JpaRepository<Space, UUID> {
    List<Space> findByUserId(UUID userId);
    Optional<Space> findByInviteCodeIgnoreCase(String inviteCode);
    void deleteByUserId(UUID userId);
}
