package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fiches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fiche {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String title;

    /** IDs des documents sources (ingestion-service), référence logique. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_document_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] sourceDocumentIds = new UUID[0];

    /**
     * Structure typée (définitions / points clés / exemples), stockée en JSONB brut.
     * Voir FicheGenerationService pour le format attendu (cf. exemple content_json du contrat).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private String contentJson;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Signalement d'obsolescence si un nouveau document est ingéré après génération (CDC §4.4). */
    @Column(name = "obsolete")
    @Builder.Default
    private boolean obsolete = false;
}
