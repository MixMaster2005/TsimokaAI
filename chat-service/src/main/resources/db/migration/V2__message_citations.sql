-- chat-service — citations lisibles pour les réponses de l'assistant (RAG).
-- JSONB : liste de {chunkId, documentId, chunkIndex, documentName, excerpt},
-- résolue UNE fois à la génération (jamais au moment de la lecture).
ALTER TABLE messages ADD COLUMN IF NOT EXISTS citations JSONB;
