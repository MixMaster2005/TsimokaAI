-- analytics-service — V2 : questions fréquentes + suivi quiz
-- Appliquée par Flyway (locations: classpath:db/migration).

-- 1. Questions fréquentes : question brute normalisée agrégée par espace.
CREATE TABLE question_frequente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id            UUID NOT NULL,
    question_normalisee VARCHAR(280) NOT NULL,
    nb_occurrences      INT NOT NULL DEFAULT 1,
    dernier_ask         TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (space_id, question_normalisee)
);

CREATE INDEX idx_question_frequente_space_id ON question_frequente(space_id);

-- 2. Suivi quiz sur la progression étudiant.
ALTER TABLE progression_etudiant ADD COLUMN nb_quiz_passes INT NOT NULL DEFAULT 0;
ALTER TABLE progression_etudiant ADD COLUMN meilleur_score DOUBLE PRECISION;
ALTER TABLE progression_etudiant ADD COLUMN dernier_score DOUBLE PRECISION;
