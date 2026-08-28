import { useRef, useState, type DragEvent } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { documentsBySpaceQueryOptions, useDocuments } from '@/features/documents/api/use-documents';
import { useUploadDocument } from '@/features/documents/api/use-upload-document';
import { useRetryDocument } from '@/features/documents/api/use-retry-document';
import { useDeleteDocument } from '@/features/documents/api/use-delete-document';
import { getMimeInfo } from '@/features/documents/mime-icons';
import type { DocumentStatus } from '@/features/documents/types';

const ACCEPTED_FORMATS =
  '.pdf,.doc,.docx,.txt,.md,.pptx,.xlsx,.xls,.csv,.html,.htm,.epub';
const ACCEPTED_FORMATS_LABEL = 'PDF, Word, PowerPoint, Excel, CSV, Markdown, HTML, EPUB';

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

const STATUS_LABEL: Record<DocumentStatus, string> = {
  PENDING: 'En attente',
  PROCESSING: 'En cours…',
  READY: 'Prêt',
  FAILED: 'Échoué',
};

function Documents() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/documents' });
  const { data: documents } = useDocuments(spaceId);
  const uploadDocument = useUploadDocument(spaceId);
  const retryDocument = useRetryDocument(spaceId);
  const deleteDocument = useDeleteDocument(spaceId);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);

  function handleDragOver(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  }

  function handleDragLeave(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    for (const file of files) {
      uploadDocument.mutate(file);
    }
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    for (const file of files) {
      uploadDocument.mutate(file);
    }
    e.target.value = '';
  }

  return (
    <div className="p-6">
      <div className="mb-4">
        <h2 className="font-display text-lg font-semibold text-foreground">Documents</h2>
        <p className="text-xs text-muted-foreground">Formats acceptés : {ACCEPTED_FORMATS_LABEL}</p>
      </div>

      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`mb-6 flex cursor-pointer flex-col items-center justify-center gap-2 rounded-fiche border-2 border-dashed p-8 text-center transition-colors ${
          isDragOver
            ? 'border-primary bg-primary/5'
            : 'border-border bg-card hover:border-primary/40 hover:bg-muted/50'
        }`}
      >
        <p className="text-sm text-muted-foreground">
          {isDragOver ? 'Déposez vos fichiers ici' : 'Glissez-déposez vos fichiers ici'}
        </p>
        <p className="text-xs text-muted-foreground">
          ou cliquez pour sélectionner ({ACCEPTED_FORMATS_LABEL})
        </p>
        {uploadDocument.isPending && (
          <p className="text-xs text-primary">Envoi en cours…</p>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept={ACCEPTED_FORMATS}
          multiple
          className="hidden"
          onChange={handleFileSelect}
        />
      </div>

      {documents?.length === 0 && (
        <div className="rounded-fiche border border-dashed border-border bg-card p-6 text-sm text-muted-foreground">
          Aucun document déposé pour l'instant. Les fiches et réponses de l'assistant seront générées à partir de ces documents.
        </div>
      )}

      <div className="flex flex-col gap-2">
        {documents?.map((doc) => {
          const mime = getMimeInfo(doc.mimeType, doc.filename);
          const MimeIcon = mime.icon;
          return (
            <div
              key={doc.id}
              className="flex items-center justify-between rounded-fiche border border-border bg-card px-3 py-2.5"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <MimeIcon className={`h-4 w-4 flex-none ${mime.color}`} aria-hidden />
                  <p className="truncate text-sm text-foreground">
                    {doc.filename}
                  </p>
                </div>
                <div className="mt-0.5 flex items-center gap-3 font-mono text-[0.65rem] text-muted-foreground">
                  <span>{doc.chunkCount ? `${doc.chunkCount} segments` : '—'}</span>
                  <span>{new Date(doc.createdAt).toLocaleDateString('fr-FR')}</span>
                </div>
                {doc.status === 'FAILED' && doc.failureReason && (
                  <p className="mt-1 text-xs text-destructive">{doc.failureReason}</p>
                )}
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
                {(doc.status === 'READY' || doc.status === 'FAILED') && (
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={deleteDocument.isPending}
                    onClick={() => {
                      if (window.confirm(`Supprimer le document « ${doc.filename} » ?`)) {
                        deleteDocument.mutate(doc.id);
                      }
                    }}
                  >
                    Supprimer
                  </Button>
                )}
                <Badge variant={STATUS_VARIANT[doc.status]}>{STATUS_LABEL[doc.status]}</Badge>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
