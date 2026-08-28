-- ingestion-service V2 : index manquants pour les performances.
-- M12 : index sur documents(status) pour le filtrage par statut (PENDING, READY, FAILED).
CREATE INDEX idx_documents_status ON documents(status);
