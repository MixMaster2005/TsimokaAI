package mg.esmia.miage.gamificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Catalogue des badges disponibles (données de référence, alimentées par
 * V2__seed_badges.sql). "code" est la clé stable utilisée par GamificationService
 * pour l'attribution (cf. BadgeCode).
 */
@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icone")
    private String icone;
}
