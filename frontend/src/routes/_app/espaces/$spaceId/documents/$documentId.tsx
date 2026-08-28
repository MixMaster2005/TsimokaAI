import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useDocument, documentQueryOptions } from '@/features/documents/api/use-document';
import { useDeleteDocument } from '@/features/documents/api/use-delete-document';
import { useRetryDocument } from '@/features/documents/api/use-retry-document';
import type { DocumentStatus } from '@/features/documents/types';

const STATUS_VARIANT: Record<DocumentStatus, 'secondary' | 'attention' | 'succes' | 'erreur'> = {
  PENDING: 'secondary',
  PROCESSING: 'attention',
  READY: 'succes',
  FAILED: 'erreur',
};

const STATUS_LABEL: Record<DocumentStatus, string> = {
  PENDING: 'En attente',
  PROCESSING: 'En cours de traitement',
  READY: 'Prêt',
  FAILED: 'Échoué',
};

export const Route = createFileRoute('/_app/espaces/$spaceId/documents/$documentId')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(documentQueryOptions(params.documentId)),
  component: DocumentDetail,
});

function DocumentDetail() {
  const { spaceId, documentId } = useParams({ from: '/_app/espaces/$spaceId/documents/$documentId' });
  const { data: doc } = useDocument(documentId);
  const deleteDocument = useDeleteDocument(spaceId);
  const retryDocument = useRetryDocument(spaceId);

  if (!doc) return null;

  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Document</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">{doc.filename}</h1>

      <div className="max-w-lg space-y-6">
        <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <h2 className="mb-3 font-display text-sm font-semibold text-encre">Informations</h2>
          <dl className="grid grid-cols-2 gap-2 text-sm">
            <dt className="text-encre-muted">Statut</dt>
            <dd><Badge variant={STATUS_VARIANT[doc.status]}>{STATUS_LABEL[doc.status]}</Badge></dd>

            <dt className="text-encre-muted">Segments</dt>
            <dd className="font-mono text-encre">{doc.chunkCount ?? '—'}</dd>

            <dt className="text-encre-muted">Type MIME</dt>
            <dd className="font-mono text-encre">{doc.mimeType ?? '—'}</dd>

            <dt className="text-encre-muted">Créé le</dt>
            <dd className="text-encre">{new Date(doc.createdAt).toLocaleDateString('fr-FR')}</dd>

            <dt className="text-encre-muted">Mis à jour</dt>
            <dd className="text-encre">{new Date(doc.updatedAt).toLocaleDateString('fr-FR')}</dd>
          </dl>
        </section>

        {doc.status === 'FAILED' && doc.failureReason && (
          <section className="rounded-fiche border border-attention/30 bg-attention/5 p-4">
            <h2 className="mb-2 font-display text-sm font-semibold text-attention">Erreur</h2>
            <p className="text-xs text-encre">{doc.failureReason}</p>
          </section>
        )}

        <div className="flex gap-2">
          {doc.status === 'FAILED' && (
            <Button
              disabled={retryDocument.isPending}
              onClick={() => retryDocument.mutate(doc.id)}
            >
              {retryDocument.isPending ? 'Relance…' : 'Relancer le traitement'}
            </Button>
          )}
          <Button
            variant="destructive"
            disabled={deleteDocument.isPending}
            onClick={() => {
              if (window.confirm(`Supprimer le document « ${doc.filename} » ?`)) {
                deleteDocument.mutate(doc.id, {
                  onSuccess: () => {
                    window.history.back();
                  },
                });
              }
            }}
          >
            {deleteDocument.isPending ? 'Suppression…' : 'Supprimer'}
          </Button>
        </div>

        <Link
          to="/_app/espaces/$spaceId/documents"
          params={{ spaceId }}
          className="text-sm text-encre-muted underline hover:text-encre"
        >
          ← Retour aux documents
        </Link>
      </div>
    </div>
  );
}
