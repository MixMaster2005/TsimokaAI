-- gamification-service — catalogue initial des badges (données de référence).
-- Les codes ci-dessous DOIVENT rester synchronisés avec BadgeCode.java.
INSERT INTO badges (id, code, nom, description, icone) VALUES
    (gen_random_uuid(), 'PREMIERE_FICHE', 'Première fiche', 'Générer sa toute première fiche de révision', 'sparkles'),
    (gen_random_uuid(), 'CINQ_FICHES', 'Assidu', 'Générer au moins 5 fiches de révision', 'flame'),
    (gen_random_uuid(), 'PREMIERE_FICHE_VALIDEE', 'Validé par l''enseignant', 'Obtenir la validation d''une fiche par un enseignant', 'check-circle'),
    (gen_random_uuid(), 'PREMIER_OBJECTIF_ATTEINT', 'Objectif atteint', 'Marquer un premier objectif de révision comme atteint', 'target');
