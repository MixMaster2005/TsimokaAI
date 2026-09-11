-- space-service — versioning du persona pédagogique (cockpit enseignant).
-- persona_version incrémentée à chaque génération/enrichissement/régénération,
-- persona_updated_at = date de dernière MAJ du persona.
ALTER TABLE spaces ADD COLUMN persona_version INT NOT NULL DEFAULT 1;
ALTER TABLE spaces ADD COLUMN persona_updated_at TIMESTAMP;
UPDATE spaces SET persona_updated_at = updated_at WHERE persona_updated_at IS NULL;
