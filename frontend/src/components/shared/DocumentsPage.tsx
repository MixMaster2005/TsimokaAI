import { useRef, useState, type DragEvent, type KeyboardEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useDocuments } from '@/features/documents/api/use-documents';
import { useDocumentSse } from '@/features/documents/api/use-document-sse';
import { useUploadDocument } from '@/features/documents/api/use-upload-document';
import { useRetryDocument } from '@/features/documents/api/use-retry-document';
import { useDeleteDocument } from '@/features/documents/api/use-delete-document';
import { getMimeInfo } from '@/features/documents/mime-icons';
import type { DocumentStatus } from '@/features/documents/types';

const ACCEPTED_FORMATS =
  '.pdf,.docx,.txt,.md,.pptx,.xlsx,.xls,.csv,.html,.htm,.epub';
const ACCEPTED_FORMATS_LABEL = 'PDF, Word, PowerPoint, Excel, CSV, Markdown, HTML, EPUB';

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

interface DocumentsPageProps {
  spaceId: string;
}

export function DocumentsPage({ spaceId }: DocumentsPageProps) {
  useDocumentSse(spaceId);
  const { data: documents, isLoading, isError, refetch } = useDocuments(spaceId);
  const uploadDocument = useUploadDocument(spaceId);
  const retryDocument = useRetryDocument(spaceId);
  const deleteDocument = useDeleteDocument(spaceId);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [docASupprimer, setDocASupprimer] = useState<{ id: string; filename: string } | null>(null);

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
    <div className="p-4 sm:p-6">
      <div className="mb-4">
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Espace</p>
        <h2 className="font-display text-lg font-semibold text-encre">Documents</h2>
        <p className="text-xs text-encre-muted">Formats acceptés : {ACCEPTED_FORMATS_LABEL}</p>
      </div>

      <div
        role="button"
        tabIndex={0}
        aria-label="Déposer des fichiers"
        aria-describedby="formats-documents"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        onKeyDown={(e: KeyboardEvent) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            fileInputRef.current?.click();
          }
        }}
        className={`mb-6 flex cursor-pointer flex-col items-center justify-center gap-2 rounded-fiche border-2 border-dashed p-8 text-center transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-within:ring-2 focus-within:ring-ring ${
          isDragOver
            ? 'border-primary bg-primary/5'
            : 'border-papier-border bg-papier-carte hover:border-primary/40 hover:bg-papier-bg'
        }`}
      >
        <p className="text-sm text-encre-muted">
          {isDragOver ? 'Dépose tes fichiers ici' : 'Glisse-dépose tes fichiers ici'}
        </p>
        <p id="formats-documents" className="text-xs text-encre-muted">
          ou clique pour sélectionner ({ACCEPTED_FORMATS_LABEL})
        </p>
        {uploadDocument.isPending && (
          <p className="text-xs text-primary" role="status" aria-live="polite">
            Envoi en cours…
          </p>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept={ACCEPTED_FORMATS}
          multiple
          className="sr-only"
          aria-label="Choisir des fichiers à déposer"
          onChange={handleFileSelect}
        />
      </div>

      {isLoading && (
        <div role="status" aria-live="polite" className="flex flex-col gap-2" aria-busy="true">
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
          <span className="sr-only">Chargement des documents…</span>
        </div>
      )}

      {isError && (
        <div className="flex flex-col items-start gap-3 rounded-fiche border border-erreur/40 bg-papier-carte p-6">
          <p role="alert" className="text-sm text-erreur">
            Impossible de charger les documents. Vérifie ta connexion puis réessaie.
          </p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Réessayer
          </Button>
        </div>
      )}

      {documents !== undefined && documents.length === 0 && (
        <div className="rounded-fiche border border-dashed border-papier-border bg-papier-carte p-6 text-sm text-encre-muted">
          Aucun document déposé pour l'instant. Dépose un fichier ci-dessus : les fiches et réponses de l'assistant seront générées à partir de ces documents.
        </div>
      )}

      <div className="flex flex-col gap-2">
        {documents?.map((doc) => {
          const mime = getMimeInfo(doc.mimeType, doc.filename);
          const MimeIcon = mime.icon;
          return (
            <div
              key={doc.id}
              className="flex items-center justify-between rounded-fiche border border-papier-border bg-papier-carte px-3 py-2.5"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <MimeIcon className={`h-4 w-4 flex-none ${mime.color}`} aria-hidden="true" />
                  <p className="truncate text-sm text-encre">
                    {doc.filename}
                  </p>
                </div>
                <div className="mt-0.5 flex items-center gap-3 font-mono text-[0.65rem] tabular-nums text-encre-muted">
                  <span>{doc.chunkCount ? `${doc.chunkCount} segments` : '—'}</span>
                  <span>{new Date(doc.createdAt).toLocaleDateString('fr-FR')}</span>
                </div>
                {doc.status === 'FAILED' && doc.failureReason && (
                  <p className="mt-1 text-xs text-erreur">{doc.failureReason}</p>
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
                    onClick={() => setDocASupprimer({ id: doc.id, filename: doc.filename })}
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

      <ConfirmDialog
        open={docASupprimer !== null}
        onOpenChange={(open) => {
          if (!open) setDocASupprimer(null);
        }}
        title="Supprimer le document"
        description={
          docASupprimer
            ? `« ${docASupprimer.filename} » — les fiches déjà générées restent, mais tu ne pourras plus régénérer depuis ce fichier.`
            : ''
        }
        confirmLabel="Supprimer"
        isPending={deleteDocument.isPending}
        onConfirm={() => {
          if (!docASupprimer) return;
          deleteDocument.mutate(docASupprimer.id, {
            onSuccess: () => setDocASupprimer(null),
          });
        }}
      />
    </div>
  );
}
