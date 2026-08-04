package mg.esmia.miage.userservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.userservice.dto.UpdateProfileRequest;
import mg.esmia.miage.userservice.dto.UserResponse;
import mg.esmia.miage.userservice.entity.User;
import mg.esmia.miage.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = findOrThrow(id);
        user.setDisplayName(request.displayName());
        return UserResponse.from(userRepository.save(user));
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}
