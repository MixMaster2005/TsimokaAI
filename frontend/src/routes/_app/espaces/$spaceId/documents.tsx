import { useRef } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { documentsBySpaceQueryOptions, useDocuments } from '@/features/documents/api/use-documents';
import { useUploadDocument } from '@/features/documents/api/use-upload-document';
import { useRetryDocument } from '@/features/documents/api/use-retry-document';
import type { DocumentStatus } from '@/features/documents/types';

export const Route = createFileRoute('/_app/espaces/$spaceId/documents')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(documentsBySpaceQueryOptions(params.spaceId)),
  component: Documents,
});

const STATUS_VARIANT: Record<DocumentStatus, 'secondary' | 'attention' | 'succes' | 'erreur'> = {
  PENDING: 'secondary',
  PROCESSING: 'attention',
  READY: 'succes',
  FAILED: 'erreur',
};

function Documents() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/documents' });
  const { data: documents } = useDocuments(spaceId);
  const uploadDocument = useUploadDocument(spaceId);
  const retryDocument = useRetryDocument(spaceId);
  const fileInputRef = useRef<HTMLInputElement>(null);

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-foreground">Documents</h2>
        <Button onClick={() => fileInputRef.current?.click()} disabled={uploadDocument.isPending}>
          {uploadDocument.isPending ? 'Envoi…' : '+ Déposer un document'}
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.doc,.docx,.txt"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) uploadDocument.mutate(file);
            e.target.value = '';
          }}
        />
      </div>

      <div className="flex flex-col gap-2">
        {documents?.map((doc) => (
          <div
            key={doc.id}
            className="flex items-center justify-between rounded-fiche border border-border bg-card px-3 py-2.5"
          >
            <div className="min-w-0">
              <p className="truncate text-sm text-foreground">{doc.filename}</p>
              <p className="font-mono text-[0.65rem] text-muted-foreground">
                {doc.chunkCount ? `${doc.chunkCount} segments` : '—'}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {doc.status === 'FAILED' && (
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={retryDocument.isPending}
                  onClick={() => retryDocument.mutate(doc.id)}
                >
                  Réessayer
                </Button>
              )}
              <Badge variant={STATUS_VARIANT[doc.status]}>{doc.status}</Badge>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
