-- space-service — schéma initial (cf. contrat "Base de projet" Notion)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE spaces (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,  -- référence logique vers user-service, pas de FK inter-service
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    subject_tag         VARCHAR(100),
    assistant_persona   TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE groupes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id    UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    nom         VARCHAR(255) NOT NULL,
    description TEXT,
    created_by  UUID,  -- référence logique vers user-service
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE membres_groupe (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    groupe_id   UUID NOT NULL REFERENCES groupes(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,  -- référence logique vers user-service
    role_groupe VARCHAR(20) NOT NULL DEFAULT 'MEMBRE',
    joined_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (groupe_id, user_id)
);

CREATE INDEX idx_spaces_user_id ON spaces(user_id);
CREATE INDEX idx_groupes_space_id ON groupes(space_id);
CREATE INDEX idx_membres_groupe_groupe_id ON membres_groupe(groupe_id);
