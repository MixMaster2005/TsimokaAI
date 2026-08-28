package mg.esmia.miage.userservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.userservice.dto.UpdateProfileRequest;
import mg.esmia.miage.userservice.dto.UserResponse;
import mg.esmia.miage.userservice.entity.User;
import mg.esmia.miage.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    /** Rôles autorisés pour l'auto-élection via onboarding / profil. */
    private static final Set<User.Role> SELF_ASSIGNABLE_ROLES = Set.of(User.Role.STUDENT, User.Role.ENSEIGNANT);

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = findOrThrow(id);
        // PATCH sémantique : seul le(s) champ(s) non nul est mis à jour.
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        // H1 : gestion du changement de rôle — seuls STUDENT et ENSEIGNANT sont auto-assignables.
        if (request.role() != null) {
            if (!SELF_ASSIGNABLE_ROLES.contains(request.role())) {
                throw new BadRequestException("Rôle non autorisé : " + request.role()
                        + ". Seuls STUDENT et ENSEIGNANT sont auto-assignables.");
            }
            user.setRole(request.role());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}
