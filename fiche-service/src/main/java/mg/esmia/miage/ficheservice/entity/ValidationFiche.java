package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_fiche")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationFiche {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "fiche_id", nullable = false, unique = true)
    private UUID ficheId;

    @Column(name = "enseignant_id", nullable = false)
    private UUID enseignantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "validated_at")
    private Instant validatedAt;

    public enum Statut {
        EN_ATTENTE, VALIDEE, REJETEE
    }
}
