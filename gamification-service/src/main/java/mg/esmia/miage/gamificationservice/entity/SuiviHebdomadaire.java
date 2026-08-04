package mg.esmia.miage.gamificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "suivi_hebdomadaire", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "space_id", "semaine_debut"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuiviHebdomadaire {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Lundi de la semaine concernée (ISO). */
    @Column(name = "semaine_debut", nullable = false)
    private LocalDate semaineDebut;

    @Column(name = "nb_fiches_generees")
    @Builder.Default
    private Integer nbFichesGenerees = 0;

    @Column(name = "nb_objectifs_atteints")
    @Builder.Default
    private Integer nbObjectifsAtteints = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
