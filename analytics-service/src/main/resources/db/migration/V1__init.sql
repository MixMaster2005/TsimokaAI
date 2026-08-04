-- analytics-service — schéma initial (cf. contrat "Base de projet" Notion)
-- Alimenté exclusivement par consommation d'événements (aucune FK vers d'autres services).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE progression_etudiant (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    space_id            UUID NOT NULL,
    taux_reussite       DOUBLE PRECISION NOT NULL DEFAULT 0,
    notions_maitrisees  JSONB NOT NULL DEFAULT '[]',
    notions_faibles     JSONB NOT NULL DEFAULT '[]',
    nb_questions_posees INT NOT NULL DEFAULT 0,
    nb_fiches_generees  INT NOT NULL DEFAULT 0,
    derniere_activite   TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, space_id)
);

CREATE TABLE statistique_espace (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id         UUID NOT NULL,
    notion           VARCHAR(255) NOT NULL,
    nb_consultations INT NOT NULL DEFAULT 0,
    nb_questions     INT NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (space_id, notion)
);

CREATE TABLE chapitre_difficile (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id         UUID NOT NULL,
    chapitre         VARCHAR(255) NOT NULL,
    score_difficulte DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (space_id, chapitre)
);

CREATE TABLE recommandations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    space_id    UUID NOT NULL,
    type        VARCHAR(50) NOT NULL,
    contenu     TEXT NOT NULL,
    generee_le  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_progression_space_id ON progression_etudiant(space_id);
CREATE INDEX idx_statistique_espace_space_id ON statistique_espace(space_id);
CREATE INDEX idx_chapitre_difficile_space_id ON chapitre_difficile(space_id);
CREATE INDEX idx_recommandations_user_space ON recommandations(user_id, space_id);
