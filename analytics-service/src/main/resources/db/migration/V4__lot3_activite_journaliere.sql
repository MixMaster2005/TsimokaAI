-- analytics-service — V4 (Lot 3) : activité journalière pour l'évolution réelle.
-- Appliquée par Flyway (locations: classpath:db/migration). Compatible ddl-auto: validate.
--
-- Une ligne par (étudiant, espace, jour UTC), alimentée par chaque événement consommé
-- (questions, fiches, quiz, corrections, validations). Sert evolution12Semaines :
-- étudiants distincts actifs par semaine calendaire, au lieu de l'ancienne heuristique
-- basée sur la seule derniereActivite (qui sous-comptait l'activité).

CREATE TABLE activite_journaliere (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    space_id     UUID NOT NULL,
    jour         DATE NOT NULL,
    nb_questions INT NOT NULL DEFAULT 0,
    nb_fiches    INT NOT NULL DEFAULT 0,
    nb_quiz      INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, space_id, jour)
);

CREATE INDEX idx_activite_space_jour ON activite_journaliere(space_id, jour);
CREATE INDEX idx_activite_user ON activite_journaliere(user_id);
