-- space-service V2 : index manquants pour les performances.
-- M4 : index sur membres_space(space_id) pour les jointures fréquentes (liste membres, vérification d'appartenance).
CREATE INDEX idx_membres_space_space_id ON membres_space(space_id);
