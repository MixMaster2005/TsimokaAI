-- chat-service — traçabilité exacte persona par message (V2 back).
-- Version du persona de l'espace ayant généré la réponse (cf. space-service Space.personaVersion).
-- Nullable : null = version inconnue (space-service injoignable) ou message antérieur à la feature.
-- Positionné uniquement sur les messages ASSISTANT ; les messages USER restent à null.
ALTER TABLE messages ADD COLUMN IF NOT EXISTS persona_version INT;
