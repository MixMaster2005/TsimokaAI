-- fiche-service V2 : index manquants pour les performances.
-- M11 : index sur partage_fiche(destinataire_id) pour la requête "mes fiches partagées".
CREATE INDEX idx_partage_fiche_destinataire_id ON partage_fiche(destinataire_id);
