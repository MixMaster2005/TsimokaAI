import { useState, type FormEvent } from 'react';
import { createFileRoute, redirect, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { useInviteCode } from '@/features/espaces/api/use-invite-code';
import { useRegenerateInviteCode } from '@/features/espaces/api/use-regenerate-invite-code';
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
  const { data: space } = useEspace(spaceId);
  const { data: inviteCode } = useInviteCode(spaceId, true);
  const regenerateInviteCode = useRegenerateInviteCode(spaceId);
  const updateEspace = useUpdateEspace(spaceId);
  const deleteEspace = useDeleteEspace(spaceId);
  const [name, setName] = useState(space?.name ?? '');
  const [description, setDescription] = useState(space?.description ?? '');
  const [subjectTag, setSubjectTag] = useState(space?.subjectTag ?? '');
  const [copied, setCopied] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    updateEspace.mutate({ name, description, subjectTag });
  }

  async function handleCopyCode() {
    if (!inviteCode) return;
    await navigator.clipboard.writeText(inviteCode.inviteCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (!space) return null;

  return (
    <div className="max-w-lg p-6">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="name">Nom</Label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="subjectTag">Tag disciplinaire</Label>
          <Input id="subjectTag" value={subjectTag} onChange={(e) => setSubjectTag(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="description">Description</Label>
          <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label>Persona pédagogique généré</Label>
          {/* Lecture seule pour l'instant — pas de endpoint de régénération identifié côté back */}
          <p className="rounded-fiche border border-border bg-secondary p-3 text-xs text-muted-foreground">
            {space.assistantPersona ?? 'Non généré pour l\u2019instant.'}
          </p>
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
                {copied ? 'Copié ✓' : 'Copier'}
              </Button>
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
        {confirmDelete ? (
          <div className="flex gap-2">
            <Button variant="destructive" onClick={() => deleteEspace.mutate()}>
              Confirmer la suppression
            </Button>
            <Button variant="outline" onClick={() => setConfirmDelete(false)}>
              Annuler
            </Button>
          </div>
        ) : (
          <Button variant="destructive" onClick={() => setConfirmDelete(true)}>
            Supprimer l'espace
          </Button>
        )}
      </div>
    </div>
  );
}
