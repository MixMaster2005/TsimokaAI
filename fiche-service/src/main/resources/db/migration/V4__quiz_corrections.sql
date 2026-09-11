-- fiche-service V4 : corrections de quiz par l'enseignant.
-- attempt_id NULL = correction portant sur le quiz lui-même,
-- sinon correction d'une tentative d'un étudiant.
CREATE TABLE quiz_corrections (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id       UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    attempt_id    UUID REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    enseignant_id UUID NOT NULL,
    commentaire   TEXT,
    score_corrige INT,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_quiz_corrections_quiz_id ON quiz_corrections(quiz_id);
CREATE INDEX idx_quiz_corrections_attempt_id ON quiz_corrections(attempt_id);
