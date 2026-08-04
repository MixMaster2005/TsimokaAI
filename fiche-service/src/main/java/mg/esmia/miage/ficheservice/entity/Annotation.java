package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "annotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Annotation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "fiche_id", nullable = false)
    private UUID ficheId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(name = "section_ref")
    private String sectionRef;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
