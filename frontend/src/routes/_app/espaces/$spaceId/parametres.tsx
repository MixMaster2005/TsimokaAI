import { useState, type FormEvent } from 'react';
import { createFileRoute, Link, redirect, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { useClipboard } from '@/hooks/use-clipboard';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { useInviteCode } from '@/features/espaces/api/use-invite-code';
import { useRegenerateInviteCode } from '@/features/espaces/api/use-regenerate-invite-code';
import { useRegeneratePersona } from '@/features/espaces/api/use-regenerate-persona';
import { useUpdateEspace } from '@/features/espaces/api/use-update-espace';
import { useDeleteEspace } from '@/features/espaces/api/use-delete-espace';
import { sessionQueryOptions } from '@/features/auth/api/use-session';

/**
 * Guard créateur/propriétaire : seuls les utilisateurs dont le userId
 * correspond au userId du space peuvent accéder à la page. Les autres
 * sont redirigés vers la page Fiches de l'espace.
 */
export const Route = createFileRoute('/_app/espaces/$spaceId/parametres')({
  beforeLoad: async ({ context: { queryClient }, params }) => {
    const [session, space] = await Promise.all([
      queryClient.ensureQueryData(sessionQueryOptions),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]);
    if (space.userId !== session.id) {
      throw redirect({ to: '/espaces/$spaceId/fiches', params: { spaceId: params.spaceId } });
    }
  },
  component: ParametresEspace,
});

function ParametresEspace() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/parametres' });
  const { data: space, isLoading } = useEspace(spaceId);
  // Pas de fetch du code d'invitation tant que l'espace n'est pas chargé
  // (évite une requête vouée au 404 quand l'espace est introuvable).
  const { data: inviteCode } = useInviteCode(spaceId, !!space);
  const regenerateInviteCode = useRegenerateInviteCode(spaceId);
  const regeneratePersona = useRegeneratePersona(spaceId);
  const updateEspace = useUpdateEspace(spaceId);
  const deleteEspace = useDeleteEspace(spaceId);
  const [name, setName] = useState(space?.name ?? '');
  const [description, setDescription] = useState(space?.description ?? '');
  const [subjectTag, setSubjectTag] = useState(space?.subjectTag ?? '');
  const { copie, echec, copier } = useClipboard();
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    updateEspace.mutate({ name, description, subjectTag });
  }

  function handleCopyCode() {
    if (!inviteCode) return;
    void copier(inviteCode.inviteCode);
  }

  if (isLoading) {
    return (
      <div className="flex max-w-lg flex-col gap-3 p-4 sm:p-6">
        <div role="status" aria-live="polite" aria-busy="true" className="flex flex-col gap-3">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-32 w-full" />
          <span className="sr-only">Chargement de l'espace…</span>
        </div>
      </div>
    );
  }

  if (!space) {
    return (
      <div className="flex max-w-lg flex-col items-start gap-3 p-6">
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Espace</p>
        <h1 className="font-display text-xl font-semibold text-encre">Espace introuvable</h1>
        <p className="text-sm text-encre-muted">
          Cet espace n'existe pas ou n'est plus disponible. Vérifie la liste de tes espaces.
        </p>
        <Link to="/">
          <Button variant="outline">Retour aux espaces</Button>
        </Link>
      </div>
    );
  }

  const personaVersion = space.personaVersion ?? 1;
  const personaUpdatedAt = space.personaUpdatedAt
    ? new Date(space.personaUpdatedAt).toLocaleString('fr-FR')
    : '—';

  return (
    <div className="max-w-lg p-4 sm:p-6">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="name">Nom</Label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="subjectTag">Tag disciplinaire</Label>
          <Input id="subjectTag" name="espace-tag" autoComplete="off" value={subjectTag} onChange={(e) => setSubjectTag(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="description">Description</Label>
          <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label>Persona pédagogique</Label>
          <p className="text-xs text-encre-muted">
            Version {personaVersion} — MAJ {personaUpdatedAt}
          </p>
          <p className="rounded-fiche border border-papier-border bg-secondary p-3 text-xs text-encre-muted">
            {space.assistantPersona ?? 'Non généré pour l\'instant.'}
          </p>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={regeneratePersona.isPending}
              onClick={() => regeneratePersona.mutate()}
            >
              {regeneratePersona.isPending ? 'Régénération…' : 'Régénérer le persona'}
            </Button>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/espaces/$spaceId/chat" params={{ spaceId }}>
                Tester dans le chat
              </Link>
            </Button>
          </div>
          {regeneratePersona.isError && (
            <p className="text-xs text-red-600">La régénération a échoué, réessaie.</p>
          )}
        </div>
        <Button type="submit" disabled={updateEspace.isPending} className="self-start">
          Enregistrer
        </Button>
      </form>

      <Separator className="my-6" />

      {inviteCode && (
        <>
          <div className="rounded-fiche border border-dashed border-papier-border bg-papier-carte p-4">
            <p className="mb-1 text-xs font-medium uppercase tracking-wide text-encre-muted">
              Code d'invitation de l'espace
            </p>
            <div className="flex flex-wrap items-center gap-3">
              <span className="font-mono text-lg tracking-[0.3em] text-encre">{inviteCode.inviteCode}</span>
              <Button variant="outline" size="sm" onClick={handleCopyCode}>
                {copie ? 'Copié ✓' : 'Copier'}
              </Button>
              <p role="status" aria-live="polite" className="sr-only">
                {copie ? 'Code copié.' : echec ?? ''}
              </p>
              <Button
                variant="ghost"
                size="sm"
                disabled={regenerateInviteCode.isPending}
                title="Le code actuel cessera de fonctionner"
                onClick={() => regenerateInviteCode.mutate()}
              >
                {regenerateInviteCode.isPending ? 'Régénération…' : 'Régénérer'}
              </Button>
            </div>
            <p className="mt-2 text-xs text-encre-muted">
              Partage ce code pour permettre aux étudiants de rejoindre cet espace de cours.
            </p>
          </div>

          <Separator className="my-6" />
        </>
      )}

      <div>
        <p className="mb-2 text-xs text-muted-foreground">
          Supprimer cet espace efface aussi ses documents, fiches et conversations. Irréversible.
        </p>
        {deleteEspace.isError && (
          <p role="alert" className="mb-2 text-xs text-destructive">
            {deleteEspace.error.message} — réessaie.
          </p>
        )}
        <Button variant="destructive" onClick={() => setConfirmDeleteOpen(true)}>
          Supprimer l'espace
        </Button>
        <ConfirmDialog
          open={confirmDeleteOpen}
          onOpenChange={(open) => {
            if (!open) setConfirmDeleteOpen(false);
          }}
          title="Supprimer l'espace"
          description="Supprimer cet espace efface aussi ses documents, fiches et conversations. Irréversible."
          confirmLabel="Supprimer l'espace"
          isPending={deleteEspace.isPending}
          onConfirm={() => deleteEspace.mutate()}
        />
      </div>
    </div>
  );
}
