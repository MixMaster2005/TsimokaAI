import { useEffect, useState, type ReactNode } from 'react';
import { Brain, RefreshCw } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useSession } from '@/features/auth/api/use-session';
import { useDocuments } from '@/features/documents/api/use-documents';
import type { DocumentStatus } from '@/features/documents/types';
import { useEspace } from '../api/use-espace';
import { useRegeneratePersona } from '../api/use-regenerate-persona';
import { useUpdateEspace } from '../api/use-update-espace';

interface PersonaModalProps {
  spaceId: string;
  /** Date ISO du message assistant ayant ouvert le modal — heuristique V1 uniquement. */
  contextMessageCreatedAt?: string | null;
  /** Déclencheur externe (bouton header, pastille message…). */
  trigger?: ReactNode;
  /** Mode contrôlé optionnel — sinon état interne. */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}

const DOC_STATUS_LABEL: Record<DocumentStatus, string> = {
  PENDING: 'En attente',
  PROCESSING: 'En traitement',
  READY: 'Prêt',
  FAILED: 'Échec',
};

function formatFr(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
}

/**
 * Modal Persona V1 — lecture (+ édition/régénération réservées au propriétaire).
 *
 * Heuristique V1 : si `contextMessageCreatedAt` < `personaUpdatedAt`, le message
 * a été répondu sous un persona antérieur (badge informatif, pas de versionnage back).
 */
export function PersonaModal({
  spaceId,
  contextMessageCreatedAt,
  trigger,
  open,
  onOpenChange,
}: PersonaModalProps) {
  const [internalOpen, setInternalOpen] = useState(false);
  const openState = open ?? internalOpen;
  const setOpen: (v: boolean) => void = onOpenChange ?? setInternalOpen;

  const { data: espace, isLoading: espaceLoading, isError: espaceError } = useEspace(spaceId);
  const { data: session } = useSession();
  const { data: documents, isLoading: docsLoading } = useDocuments(spaceId);
  const updateEspace = useUpdateEspace(spaceId);
  const regenerate = useRegeneratePersona(spaceId);

  const isOwner = espace
    ? (espace.owner ?? (session?.id !== undefined && espace.userId === session.id))
    : false;

  // Brouillon d'édition — initialisé au persona courant, réinitialisé à la fermeture.
  const [draft, setDraft] = useState<string | null>(null);
  useEffect(() => {
    if (!openState) setDraft(null);
  }, [openState]);

  const persona = espace?.assistantPersona ?? '';
  const value = draft ?? persona;
  const dirty = draft !== null && draft !== persona;

  const answeredUnderOlderPersona =
    !!contextMessageCreatedAt &&
    !!espace?.personaUpdatedAt &&
    new Date(contextMessageCreatedAt).getTime() < new Date(espace.personaUpdatedAt).getTime();

  return (
    <Dialog open={openState} onOpenChange={setOpen}>
      {trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Brain className="size-4 text-muted-foreground" />
            Persona pédagogique
          </DialogTitle>
          <DialogDescription>
            Registre disciplinaire actif de l'assistant pour cet espace.
            {!isOwner && ' Lecture seule — seul le propriétaire peut le modifier.'}
          </DialogDescription>
        </DialogHeader>

        {espaceLoading && (
          <p className="text-sm text-muted-foreground">Chargement du persona…</p>
        )}
        {espaceError && (
          <p className="text-sm text-destructive">Impossible de charger le persona.</p>
        )}

        {espace && (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center gap-2">
              {typeof espace.personaVersion === 'number' && (
                <Badge variant="secondary">v{espace.personaVersion}</Badge>
              )}
              {espace.subjectTag && <Badge variant="outline">{espace.subjectTag}</Badge>}
              {answeredUnderOlderPersona && (
                <Badge variant="attention">Répondu sous un persona antérieur</Badge>
              )}
            </div>

            <p className="text-xs text-muted-foreground">
              Mis à jour le {formatFr(espace.personaUpdatedAt ?? espace.updatedAt)}
              {' · '}Espace créé le {formatFr(espace.createdAt)}
            </p>

            <div className="flex flex-col gap-1.5">
              <Label>Persona actif</Label>
              <div className="max-h-48 overflow-y-auto rounded-md border border-border bg-muted/40 px-3 py-2">
                {persona ? (
                  <p className="whitespace-pre-wrap text-sm text-foreground">{persona}</p>
                ) : (
                  <p className="text-sm text-muted-foreground">Aucun persona défini.</p>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label>Documents sources ({docsLoading ? '…' : (documents?.length ?? 0)})</Label>
              {docsLoading ? (
                <p className="text-sm text-muted-foreground">Chargement des documents…</p>
              ) : documents && documents.length > 0 ? (
                <ul className="max-h-28 overflow-y-auto rounded-md border border-border px-3 py-2 text-sm">
                  {documents.map((doc) => (
                    <li key={doc.id} className="flex items-center justify-between gap-2 py-0.5">
                      <span className="truncate text-foreground" title={doc.filename}>
                        {doc.filename}
                      </span>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        {DOC_STATUS_LABEL[doc.status]}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">Aucun document dans cet espace.</p>
              )}
            </div>

            {isOwner && (
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="persona-edit">Modifier le persona</Label>
                <textarea
                  id="persona-edit"
                  rows={5}
                  value={value}
                  onChange={(e) => setDraft(e.target.value)}
                  placeholder="Décrivez le registre disciplinaire attendu…"
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
                {(updateEspace.isError || regenerate.isError) && (
                  <p className="text-sm text-destructive">
                    {updateEspace.isError
                      ? "Échec de l'enregistrement — réessaie."
                      : 'Échec de la régénération.'}
                  </p>
                )}
                {(updateEspace.isSuccess || regenerate.isSuccess) && (
                  <p className="text-sm text-muted-foreground">Persona mis à jour.</p>
                )}
              </div>
            )}
          </div>
        )}

        {isOwner && espace && (
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={regenerate.isPending}
              onClick={() => regenerate.mutate()}
            >
              <RefreshCw className="size-4" />
              {regenerate.isPending ? 'Régénération…' : 'Régénérer'}
            </Button>
            <Button
              type="button"
              disabled={!dirty || updateEspace.isPending}
              onClick={() => draft !== null && updateEspace.mutate({ assistantPersona: draft })}
            >
              {updateEspace.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
}
