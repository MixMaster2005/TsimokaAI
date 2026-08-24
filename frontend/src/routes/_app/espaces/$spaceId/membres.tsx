import { useState, type FormEvent } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { groupesBySpaceQueryOptions, useCreateGroupe, useGroupes } from '@/features/groupes/api/use-groupes';

export const Route = createFileRoute('/_app/espaces/$spaceId/membres')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(groupesBySpaceQueryOptions(params.spaceId)),
  component: Membres,
});

function Membres() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/membres' });
  const { data: groupes } = useGroupes(spaceId);
  const createGroupe = useCreateGroupe(spaceId);
  const [nom, setNom] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!nom.trim()) return;
    createGroupe.mutate({ nom }, { onSuccess: () => setNom('') });
  }

  return (
    <div className="p-6">
      <h2 className="mb-4 font-display text-lg font-semibold text-foreground">Groupes de travail</h2>

      <div className="mb-6 flex flex-col gap-2">
        {groupes?.map((g) => (
          <div key={g.id} className="rounded-fiche border border-border bg-card p-3">
            <p className="text-sm font-medium text-foreground">{g.nom}</p>
            {g.description && <p className="text-xs text-muted-foreground">{g.description}</p>}
            {/* Liste des membres du groupe (MembreGroupeResponse) — à ajouter,
                GET /api/v1/groupes/{groupeId}/membres */}
          </div>
        ))}
        {groupes?.length === 0 && <p className="text-sm text-muted-foreground">Aucun groupe pour l\u2019instant.</p>}
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input placeholder="Nom du groupe" value={nom} onChange={(e) => setNom(e.target.value)} />
        <Button type="submit" disabled={createGroupe.isPending}>
          Créer un groupe
        </Button>
      </form>

      {/* Liste des membres de l'ESPACE (pas des groupes) : aucun endpoint dédié
          identifié dans SpaceController — probablement à ajouter côté back
          (ex: GET /api/v1/spaces/{id}/membres) si ce n'est pas déjà prévu ailleurs. */}
    </div>
  );
}
