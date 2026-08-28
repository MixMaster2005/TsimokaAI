import { useState, type FormEvent } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { useInviteCode } from '@/features/espaces/api/use-invite-code';
import { useRegenerateInviteCode } from '@/features/espaces/api/use-regenerate-invite-code';
import { useRemoveMembre } from '@/features/espaces/api/use-remove-membre';
import { useLeaveEspace } from '@/features/espaces/api/use-leave-espace';
import { useEspace } from '@/features/espaces/api/use-espace';
import { membresQueryOptions, useMembres } from '@/features/espaces/api/use-membres';
import { groupesBySpaceQueryOptions } from '@/features/groupes/api/keys';
import { useCreateGroupe } from '@/features/groupes/api/use-create-groupe';
import { useDeleteGroupe } from '@/features/groupes/api/use-delete-groupe';
import { useAddMembreGroupe } from '@/features/groupes/api/use-add-membre-groupe';
import { useGroupes } from '@/features/groupes/api/use-groupes';
import { useMembresGroupe } from '@/features/groupes/api/use-membres-groupe';
import type { Groupe } from '@/features/groupes/types';

export const Route = createFileRoute('/_app/espaces/$spaceId/membres')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(groupesBySpaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(membresQueryOptions(params.spaceId)),
    ]),
  component: Membres,
});

function Membres() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/membres' });
  const { data: espace } = useEspace(spaceId);
  const isOwner = Boolean(espace?.owner);
  const leaveEspace = useLeaveEspace();

  return (
    <div className="p-6">
      <h2 className="mb-4 font-display text-lg font-semibold text-foreground">Groupes de travail</h2>
      <GroupesSection spaceId={spaceId} />

      <Separator className="my-8" />

      <h2 className="mb-4 font-display text-lg font-semibold text-foreground">Membres de l'espace</h2>
      {isOwner ? <InviteCodeSection spaceId={spaceId} /> : null}
      <MembresSection spaceId={spaceId} isOwner={isOwner} />

      {!isOwner && (
        <>
          <Separator className="my-8" />
          <Button
            variant="destructive"
            disabled={leaveEspace.isPending}
            onClick={() => {
              if (window.confirm('Quitter cet espace ? Tu perdras l\'accès à ses fiches et conversations.')) {
                leaveEspace.mutate(spaceId, {
                  onSuccess: () => {
                    window.location.href = '/';
                  },
                });
              }
            }}
          >
            {leaveEspace.isPending ? 'Depart…' : 'Quitter l\'espace'}
          </Button>
        </>
      )}
    </div>
  );
}

function GroupeCard({ groupe, spaceId }: { groupe: Groupe; spaceId: string }) {
  const { data: membres } = useMembresGroupe(groupe.id);
  const deleteGroupe = useDeleteGroupe(spaceId);
  const addMembre = useAddMembreGroupe(groupe.id);
  const [newUserId, setNewUserId] = useState('');

  function handleAddMembre(e: FormEvent) {
    e.preventDefault();
    if (!newUserId.trim()) return;
    addMembre.mutate(
      { userId: newUserId.trim() },
      { onSuccess: () => setNewUserId('') },
    );
  }

  return (
    <div className="rounded-fiche border border-border bg-card p-3">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-foreground">{groupe.nom}</p>
          {groupe.description && <p className="mt-0.5 text-xs text-muted-foreground">{groupe.description}</p>}
        </div>
        <div className="flex items-center gap-2">
          <span className="font-mono text-[0.65rem] text-muted-foreground">
            {membres?.length ?? 0} membre{(membres?.length ?? 0) > 1 ? 's' : ''}
          </span>
          <Button
            variant="ghost"
            size="sm"
            disabled={deleteGroupe.isPending}
            onClick={() => {
              if (window.confirm(`Supprimer le groupe « ${groupe.nom} » ?`)) {
                deleteGroupe.mutate(groupe.id);
              }
            }}
          >
            Supprimer
          </Button>
        </div>
      </div>

      {membres && membres.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5 border-t border-dashed border-border pt-2">
          {membres.map((m) => (
            <span
              key={m.id}
              className="inline-flex items-center gap-1.5 rounded-sm bg-secondary px-2 py-0.5 font-mono text-[0.65rem] text-foreground"
            >
              <span>{m.userId.slice(0, 8)}…</span>
              <span className="text-[0.6rem] uppercase text-muted-foreground">({m.roleGroupe})</span>
            </span>
          ))}
        </div>
      )}

      <form onSubmit={handleAddMembre} className="mt-3 flex gap-2">
        <Input
          placeholder="ID utilisateur à ajouter"
          value={newUserId}
          onChange={(e) => setNewUserId(e.target.value)}
          className="flex-1 font-mono text-xs"
        />
        <Button type="submit" size="sm" disabled={addMembre.isPending || !newUserId.trim()}>
          {addMembre.isPending ? '…' : 'Ajouter'}
        </Button>
      </form>
    </div>
  );
}

function GroupesSection({ spaceId }: { spaceId: string }) {
  const { data: groupes } = useGroupes(spaceId);
  const createGroupe = useCreateGroupe(spaceId);
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!nom.trim()) return;
    createGroupe.mutate(
      {
        nom: nom.trim(),
        ...(description.trim() ? { description: description.trim() } : {}),
      },
      {
        onSuccess: () => {
          setNom('');
          setDescription('');
        },
      },
    );
  }

  return (
    <>
      <div className="mb-6 flex flex-col gap-2">
        {groupes?.map((g) => (
          <GroupeCard key={g.id} groupe={g} spaceId={spaceId} />
        ))}
        {groupes?.length === 0 && <p className="text-sm text-muted-foreground">Aucun groupe pour l'instant.</p>}
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-2 sm:flex-row">
        <Input
          placeholder="Nom du groupe"
          value={nom}
          onChange={(e) => setNom(e.target.value)}
          className="flex-1"
          required
        />
        <Input
          placeholder="Description (optionnelle)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="flex-1"
        />
        <Button type="submit" disabled={createGroupe.isPending || !nom.trim()}>
          {createGroupe.isPending ? 'Création…' : 'Créer un groupe'}
        </Button>
      </form>
    </>
  );
}

/** Code d'invitation — visible du propriétaire uniquement (endpoint 403 pour les autres). */
function InviteCodeSection({ spaceId }: { spaceId: string }) {
  const { data: inviteCode } = useInviteCode(spaceId, true);
  const regenerate = useRegenerateInviteCode(spaceId);
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    if (!inviteCode) return;
    await navigator.clipboard.writeText(inviteCode.inviteCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (!inviteCode) return null;

  return (
    <div className="mb-6 rounded-fiche border border-dashed border-papier-border bg-papier-carte p-4">
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-encre-muted">
        Code d'invitation
      </p>
      <div className="flex flex-wrap items-center gap-3">
        {/* Plex Mono, très espacé : le code se lit à voix haute ou se recopie à la main */}
        <span className="font-mono text-lg tracking-[0.3em] text-encre">{inviteCode.inviteCode}</span>
        <Button variant="outline" size="sm" onClick={handleCopy}>
          {copied ? 'Copié ✓' : 'Copier'}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={regenerate.isPending}
          title="Le code actuel cessera de fonctionner"
          onClick={() => regenerate.mutate()}
        >
          {regenerate.isPending ? 'Régénération…' : 'Régénérer'}
        </Button>
      </div>
      <p className="mt-2 text-xs text-encre-muted">
        Partage ce code pour donner accès à ton espace en lecture et participation.
      </p>
    </div>
  );
}

function MembresSection({ spaceId, isOwner }: { spaceId: string; isOwner: boolean }) {
  const { data: membres } = useMembres(spaceId);
  const removeMembre = useRemoveMembre(spaceId);

  if (membres?.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Aucun membre extérieur pour l'instant{isOwner ? ' — partage ton code ci-dessus.' : '.'}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {membres?.map((m) => (
        <div key={m.id} className="flex items-center gap-3 rounded-fiche border border-border bg-card p-3">
          <div className="min-w-0 flex-1">
            {/* Pas de nom lisible : user-service n'expose pas de résolution batch
                d'utilisateurs — identifiant tronqué en attendant (limitation connue). */}
            <p className="truncate font-mono text-xs text-foreground">
              {m.userId.slice(0, 8)}…
            </p>
            <p className="text-[0.68rem] text-muted-foreground">
              membre depuis le {new Date(m.joinedAt).toLocaleDateString('fr-FR')}
            </p>
          </div>
          {isOwner && (
            <Button
              variant="ghost"
              size="sm"
              disabled={removeMembre.isPending}
              onClick={() => removeMembre.mutate(m.userId)}
            >
              Retirer
            </Button>
          )}
        </div>
      ))}
    </div>
  );
}
