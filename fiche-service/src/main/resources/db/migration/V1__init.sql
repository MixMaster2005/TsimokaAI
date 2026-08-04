-- fiche-service — schéma initial (cf. contrat "Base de projet" Notion)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE fiches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id            UUID NOT NULL,   -- référence logique vers space-service
    user_id             UUID NOT NULL,   -- référence logique vers user-service
    title               VARCHAR(255),
    source_document_ids UUID[] NOT NULL DEFAULT '{}',
    content_json        JSONB,
    obsolete            BOOLEAN NOT NULL DEFAULT false,
    generated_at        TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE partage_fiche (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fiche_id        UUID NOT NULL REFERENCES fiches(id) ON DELETE CASCADE,
    groupe_id       UUID,   -- référence logique vers space-service, nullable
    destinataire_id UUID,   -- référence logique vers user-service, nullable
    partage_par     UUID NOT NULL,
    shared_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE annotations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fiche_id    UUID NOT NULL REFERENCES fiches(id) ON DELETE CASCADE,
    auteur_id   UUID NOT NULL,
    contenu     TEXT NOT NULL,
    section_ref VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE validation_fiche (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fiche_id      UUID NOT NULL UNIQUE REFERENCES fiches(id) ON DELETE CASCADE,
    enseignant_id UUID NOT NULL,
    statut        VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire   TEXT,
    validated_at  TIMESTAMP
);

CREATE INDEX idx_fiches_space_user ON fiches(space_id, user_id);
CREATE INDEX idx_partage_fiche_fiche_id ON partage_fiche(fiche_id);
CREATE INDEX idx_annotations_fiche_id ON annotations(fiche_id);
