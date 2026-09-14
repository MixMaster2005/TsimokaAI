package mg.esmia.miage.ficheservice.service;

import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.ValidateFicheRequest;
import mg.esmia.miage.ficheservice.dto.ValidationResponse;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import mg.esmia.miage.ficheservice.repository.FicheRepository;
import mg.esmia.miage.ficheservice.repository.ValidationFicheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ValidationFicheRepository validationFicheRepository;

    @Mock
    private FicheRepository ficheRepository;

    @Mock
    private RedisEventPublisher eventPublisher;

    @InjectMocks
    private ValidationService validationService;

    @Test
    void rejeteeSansCommentaire_throwsBadRequest() {
        UUID ficheId = UUID.randomUUID();
        UUID enseignantId = UUID.randomUUID();

        assertThrows(BadRequestException.class, () ->
                validationService.validate(ficheId, enseignantId,
                        new ValidateFicheRequest(ValidationFiche.Statut.REJETEE, null)));
        verifyNoInteractions(validationFicheRepository);
    }

    @Test
    void rejeteeBlank_throwsBadRequest() {
        UUID ficheId = UUID.randomUUID();
        UUID enseignantId = UUID.randomUUID();

        assertThrows(BadRequestException.class, () ->
                validationService.validate(ficheId, enseignantId,
                        new ValidateFicheRequest(ValidationFiche.Statut.REJETEE, "   ")));
        verifyNoInteractions(validationFicheRepository);
    }

    @Test
    void rejeteeAvecCommentaire_ok() {
        UUID ficheId = UUID.randomUUID();
        UUID enseignantId = UUID.randomUUID();
        when(validationFicheRepository.findByFicheId(ficheId)).thenReturn(Optional.empty());
        when(validationFicheRepository.save(any(ValidationFiche.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(ficheRepository.findById(ficheId)).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validate(ficheId, enseignantId,
                new ValidateFicheRequest(ValidationFiche.Statut.REJETEE, "Hors sujet"));

        assertEquals(ValidationFiche.Statut.REJETEE, response.statut());
        assertEquals("Hors sujet", response.commentaire());
    }

    @Test
    void valideeSansCommentaire_ok() {
        UUID ficheId = UUID.randomUUID();
        UUID enseignantId = UUID.randomUUID();
        when(validationFicheRepository.findByFicheId(ficheId)).thenReturn(Optional.empty());
        when(validationFicheRepository.save(any(ValidationFiche.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(ficheRepository.findById(ficheId)).thenReturn(Optional.empty());

        ValidationResponse response = validationService.validate(ficheId, enseignantId,
                new ValidateFicheRequest(ValidationFiche.Statut.VALIDEE, null));

        assertEquals(ValidationFiche.Statut.VALIDEE, response.statut());
        assertNull(response.commentaire());
    }
}
