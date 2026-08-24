-- space-service — espaces partagés : code d'invitation + membres.
--
-- L'espace reste mono-PROPRIETAIRE (spaces.user_id inchangé, écriture réservée) ;
-- on ajoute une adhésion en lecture/participation via un code d'invitation.

-- 1. Code d'invitation sur l'espace (généré à la création par SpaceService).
--    Backfill des lignes existantes : 10 caractères hexadécimaux (40 bits,
--    largement suffisant à l'échelle du mémoire), déterministe et sans collision
--    pratique ; md5(random()) ne requiert rien de plus que ce que V1 a déjà créé.
ALTER TABLE spaces ADD COLUMN invite_code VARCHAR(10);
UPDATE spaces SET invite_code = upper(substring(md5(random()::text || id::text) from 1 for 10));
ALTER TABLE spaces ALTER COLUMN invite_code SET NOT NULL;
CREATE UNIQUE INDEX idx_spaces_invite_code ON spaces(invite_code);

-- 2. Adhésions. Même convention que V1 : pas de FK inter-service sur user_id
--    (référence logique vers user-service), FK locale vers spaces avec cascade.
CREATE TABLE membres_space (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id  UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL,  -- référence logique vers user-service
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (space_id, user_id)
);

CREATE INDEX idx_membres_space_user_id ON membres_space(user_id);
