import { useState, type FormEvent } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import {
  useInviteCode,
  useRegenerateInviteCode,
  useRemoveMembre,
} from '@/features/espaces/api/use-invite-code';
import { useEspace } from '@/features/espaces/api/use-espace';
import { membresQueryOptions, useMembres } from '@/features/espaces/api/use-membres';
import { groupesBySpaceQueryOptions, useCreateGroupe, useGroupes } from '@/features/groupes/api/use-groupes';

export const Route = createFileRoute('/_app/espaces/$spaceId/membres')({
  loader: ({ context: { queryClient }, params }) => {
    // Les groupes et les membres partagent cette page : on précharge les deux.
    queryClient.ensureQueryData(groupesBySpaceQueryOptions(params.spaceId));
    return queryClient.ensureQueryData(membresQueryOptions(params.spaceId));
  },
  component: Membres,
});

function Membres() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/membres' });
  const { data: espace } = useEspace(spaceId);
  const isOwner = Boolean(espace?.owner);

  return (
    <div className="p-6">
      <h2 className="mb-4 font-display text-lg font-semibold text-foreground">Groupes de travail</h2>
      <GroupesSection spaceId={spaceId} />

      <Separator className="my-8" />

      <h2 className="mb-4 font-display text-lg font-semibold text-foreground">Membres de l'espace</h2>
      {isOwner ? <InviteCodeSection spaceId={spaceId} /> : null}
      <MembresSection spaceId={spaceId} isOwner={isOwner} />
    </div>
  );
}

function GroupesSection({ spaceId }: { spaceId: string }) {
  const { data: groupes } = useGroupes(spaceId);
  const createGroupe = useCreateGroupe(spaceId);
  const [nom, setNom] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!nom.trim()) return;
    createGroupe.mutate({ nom }, { onSuccess: () => setNom('') });
  }

  return (
    <>
      <div className="mb-6 flex flex-col gap-2">
        {groupes?.map((g) => (
          <div key={g.id} className="rounded-fiche border border-border bg-card p-3">
            <p className="text-sm font-medium text-foreground">{g.nom}</p>
            {g.description && <p className="text-xs text-muted-foreground">{g.description}</p>}
            {/* Liste des membres du groupe (MembreGroupeResponse) — à ajouter,
                GET /api/v1/groupes/{groupeId}/membres */}
          </div>
        ))}
        {groupes?.length === 0 && <p className="text-sm text-muted-foreground">Aucun groupe pour l'instant.</p>}
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input placeholder="Nom du groupe" value={nom} onChange={(e) => setNom(e.target.value)} />
        <Button type="submit" disabled={createGroupe.isPending}>
          Créer un groupe
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
