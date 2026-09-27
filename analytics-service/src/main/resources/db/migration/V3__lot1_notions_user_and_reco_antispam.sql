-- analytics-service — V3 (Lot 1) : notions par étudiant + anti-spam recommandations.
-- Appliquée par Flyway (locations: classpath:db/migration). Compatible ddl-auto: validate.

-- 1. Notions par étudiant : l'ancienne statistique_espace reste globale (dashboard
-- enseignant) ; cette table porte le compteur par (user, space, notion) utilisé pour
-- notionsFaibles / notionsMaitrisees du dashboard étudiant.
CREATE TABLE statistique_notion_user (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    space_id     UUID NOT NULL,
    notion       VARCHAR(255) NOT NULL,
    nb_questions INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, space_id, notion)
);

CREATE INDEX idx_stat_notion_user_user_space ON statistique_notion_user(user_id, space_id);
CREATE INDEX idx_stat_notion_user_space ON statistique_notion_user(space_id);

-- 2. Recommandations : hash du contenu (anti-doublon) + accusé de lecture.
ALTER TABLE recommandations ADD COLUMN contenu_hash VARCHAR(64);
ALTER TABLE recommandations ADD COLUMN lue_le TIMESTAMP NULL;

-- Backfill des lignes existantes (md5 -> 32 car., tient dans VARCHAR(64)).
UPDATE recommandations SET contenu_hash = md5(contenu) WHERE contenu_hash IS NULL;

CREATE INDEX idx_reco_user_space_type_hash ON recommandations(user_id, space_id, type, contenu_hash);
CREATE INDEX idx_reco_generee_le ON recommandations(generee_le);
