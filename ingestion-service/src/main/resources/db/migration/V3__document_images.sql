CREATE TABLE document_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    storage_url     VARCHAR(500) NOT NULL,
    placeholder_id  VARCHAR(50),
    caption         TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_document_images_document_id ON document_images(document_id);

CREATE UNIQUE INDEX idx_document_images_placeholder
  ON document_images(document_id, placeholder_id)
  WHERE placeholder_id IS NOT NULL;
