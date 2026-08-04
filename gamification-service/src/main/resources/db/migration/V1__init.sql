-- gamification-service — schéma initial (cf. contrat "Base de projet" Notion)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE objectif_revision (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    space_id       UUID NOT NULL,
    titre          VARCHAR(255) NOT NULL,
    description    TEXT,
    date_echeance  DATE,
    statut         VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE badges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    nom         VARCHAR(255) NOT NULL,
    description TEXT,
    icone       VARCHAR(255)
);

CREATE TABLE badges_obtenus (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID NOT NULL,
    badge_id  UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    obtenu_le TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, badge_id)
);

CREATE TABLE suivi_hebdomadaire (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL,
    space_id              UUID NOT NULL,
    semaine_debut         DATE NOT NULL,
    nb_fiches_generees    INT NOT NULL DEFAULT 0,
    nb_objectifs_atteints INT NOT NULL DEFAULT 0,
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, space_id, semaine_debut)
);

CREATE TABLE rappels (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    space_id   UUID,
    message    VARCHAR(500) NOT NULL,
    prevu_le   TIMESTAMP NOT NULL,
    envoye     BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_objectif_user_space ON objectif_revision(user_id, space_id);
CREATE INDEX idx_badges_obtenus_user_id ON badges_obtenus(user_id);
CREATE INDEX idx_suivi_user_space ON suivi_hebdomadaire(user_id, space_id);
CREATE INDEX idx_rappels_due ON rappels(envoye, prevu_le);
