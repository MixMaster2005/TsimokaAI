package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partage_fiche")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartageFiche {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "fiche_id", nullable = false)
    private UUID ficheId;

    @Column(name = "groupe_id")
    private UUID groupeId;

    @Column(name = "destinataire_id")
    private UUID destinataireId;

    @Column(name = "partage_par", nullable = false)
    private UUID partagePar;

    @CreationTimestamp
    @Column(name = "shared_at", updatable = false)
    private Instant sharedAt;
}
