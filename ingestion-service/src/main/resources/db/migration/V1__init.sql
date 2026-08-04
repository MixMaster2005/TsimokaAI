-- ingestion-service — schéma initial (cf. contrat "Base de projet" Notion)
-- NB: les vecteurs eux-mêmes vivent dans Qdrant (collection chunks_{space_id}), pas ici.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID NOT NULL,   -- référence logique vers space-service
    user_id         UUID NOT NULL,   -- référence logique vers user-service
    filename        VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(150),
    storage_url     VARCHAR(1000) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    chunk_count     INT NOT NULL DEFAULT 0,
    failure_reason  TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    token_count     INT,
    vector_id       VARCHAR(255)   -- id du point dans Qdrant
);

CREATE INDEX idx_documents_space_id ON documents(space_id);
CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_chunks_document_id ON chunks(document_id);
