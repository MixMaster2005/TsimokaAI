-- V5 : statut BROUILLON/PUBLIE sur les quizzes (défaut PUBLIE pour l'existant).
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'PUBLIE';
