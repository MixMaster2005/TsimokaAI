import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge as StatutBadge } from '@/components/ui/badge';
import { useEspaces } from '@/features/espaces/api/use-espaces';
import {
  useCreateObjectif,
  useObjectifs,
  useUpdateObjectifStatut,
} from '@/features/objectifs/api/use-objectifs';
import { useBadges } from '@/features/gamification/api/use-badges';
import { useRappels } from '@/features/gamification/api/use-rappels';
import type { StatutObjectif } from '@/features/objectifs/types';

/**
 * ⚠️ Même limitation architecturale que mes-fiches.tsx : ObjectifController
 * exige un spaceId (pas de "tous mes objectifs" transverse). Badges et
 * rappels, eux, sont bien transverses côté back (BadgeController.list(),
 * RappelController.listMine() sans param) — donc cette page mélange :
 * badges/rappels réellement transverses + objectifs limités à UN espace
 * choisi via le sélecteur ci-dessous, en attendant une décision sur
 * l'ajout d'un endpoint transverse côté gamification-service.
 */
export const Route = createFileRoute('/_app/objectifs')({
  component: Objectifs,
});

const STATUT_VARIANT: Record<StatutObjectif, 'secondary' | 'succes' | 'erreur'> = {
  EN_COURS: 'secondary',
  ATTEINT: 'succes',
  ABANDONNE: 'erreur',
};

function statutLabel(statut: StatutObjectif) {
  return { EN_COURS: 'En cours', ATTEINT: 'Atteint', ABANDONNE: 'Abandonné' }[statut];
}

function Objectifs() {
  const { data: espaces } = useEspaces();
  const [spaceId, setSpaceId] = useState<string | null>(null);
  const activeSpaceId = spaceId ?? espaces?.[0]?.id ?? null;

  const { data: objectifs } = useObjectifs(activeSpaceId ?? '');
  const { data: badges } = useBadges();
  const { data: rappels } = useRappels();
  const createObjectif = useCreateObjectif();
  const updateStatut = useUpdateObjectifStatut(activeSpaceId ?? '');
  const [titre, setTitre] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!activeSpaceId || !titre.trim()) return;
    createObjectif.mutate({ spaceId: activeSpaceId, titre }, { onSuccess: () => setTitre('') });
  }

  return (
    <div className="grid grid-cols-1 gap-6 p-8 lg:grid-cols-2">
      <div>
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Motivation</p>
        <h1 className="mb-4 font-display text-2xl font-semibold text-encre">Objectifs</h1>

        {espaces && espaces.length > 1 && (
          <select
            value={activeSpaceId ?? ''}
            onChange={(e) => setSpaceId(e.target.value)}
            className="mb-3 rounded-md border border-papier-border bg-papier-carte px-2 py-1.5 text-sm"
          >
            {espaces.map((e) => (
              <option key={e.id} value={e.id}>
                {e.name}
              </option>
            ))}
          </select>
        )}

        <div className="mb-4 flex flex-col gap-2">
          {objectifs?.map((o) => (
            <div
              key={o.id}
              className="flex items-center justify-between rounded-fiche border border-papier-border bg-papier-carte px-3 py-2.5"
            >
              <span className="text-sm text-encre">{o.titre}</span>
              <div className="flex items-center gap-2">
                <StatutBadge variant={STATUT_VARIANT[o.statut]}>{statutLabel(o.statut)}</StatutBadge>
                <select
                  value={o.statut}
                  onChange={(e) => updateStatut.mutate({ id: o.id, statut: e.target.value as StatutObjectif })}
                  className="bg-transparent text-xs"
                >
                  <option value="EN_COURS">En cours</option>
                  <option value="ATTEINT">Atteint</option>
                  <option value="ABANDONNE">Abandonné</option>
                </select>
              </div>
            </div>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex gap-2">
          <Input placeholder="Nouvel objectif" value={titre} onChange={(e) => setTitre(e.target.value)} />
          <Button type="submit" disabled={!activeSpaceId || createObjectif.isPending}>
            Ajouter
          </Button>
        </form>
      </div>

      <div>
        <h2 className="mb-4 font-display text-lg font-semibold text-encre">Badges</h2>
        <div className="mb-6 flex flex-wrap gap-3">
          {badges?.map((b) => (
            <div
              key={b.id}
              className={`flex size-16 flex-col items-center justify-center rounded-full text-center font-mono text-[0.55rem] uppercase leading-tight ${
                b.obtenu ? 'bg-tag-sciences text-white' : 'border-2 border-dashed border-papier-border text-encre-muted'
              }`}
              title={b.description}
            >
              {b.nom}
            </div>
          ))}
        </div>

        <h2 className="mb-3 font-display text-lg font-semibold text-encre">Rappels</h2>
        <div className="flex flex-col gap-2">
          {rappels?.map((r) => (
            <div key={r.id} className="flex items-center justify-between rounded-fiche border border-papier-border bg-papier-carte px-3 py-2 text-sm">
              <span>{r.message}</span>
              <StatutBadge variant={r.envoye ? 'secondary' : 'attention'}>
                {new Date(r.prevuLe).toLocaleDateString('fr-FR')}
              </StatutBadge>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
