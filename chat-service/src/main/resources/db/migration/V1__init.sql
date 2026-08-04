-- chat-service — schéma initial (cf. contrat "Base de projet" Notion)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id    UUID NOT NULL,   -- référence logique vers space-service
    user_id     UUID NOT NULL,   -- référence logique vers user-service
    title       VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id     UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    content             TEXT NOT NULL,
    retrieved_chunk_ids UUID[] NOT NULL DEFAULT '{}',
    model_used          VARCHAR(100),
    token_count         INT,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_space_user ON conversations(space_id, user_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
