-- V3 : Quiz comme artefact pédagogique indépendant

CREATE TABLE quizzes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id            UUID NOT NULL,
    user_id             UUID NOT NULL,
    title               VARCHAR(255),
    scope               VARCHAR(20) NOT NULL DEFAULT 'DOCUMENT',
    target_document_id  UUID,
    target_topic        VARCHAR(255),
    source_fiche_id     UUID,
    source_document_ids UUID[] NOT NULL DEFAULT '{}',
    difficulty          VARCHAR(20) NOT NULL DEFAULT 'MOYEN',
    question_count      INT NOT NULL DEFAULT 10,
    content_json        JSONB,
    obsolete            BOOLEAN NOT NULL DEFAULT false,
    generated_at        TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE quiz_attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id         UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    answers_json    JSONB,
    score           INT NOT NULL DEFAULT 0,
    total_questions INT NOT NULL,
    attempted_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE quiz_shares (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id         UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    groupe_id       UUID,
    destinataire_id UUID,
    partage_par     UUID NOT NULL,
    shared_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_quizzes_space_user ON quizzes(space_id, user_id);
CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);
CREATE INDEX idx_quiz_shares_quiz_id ON quiz_shares(quiz_id);
CREATE INDEX idx_quiz_shares_destinataire_id ON quiz_shares(destinataire_id);
