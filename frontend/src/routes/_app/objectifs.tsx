import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge as StatutBadge } from '@/components/ui/badge';
import { useEspaces } from '@/features/espaces/api/use-espaces';
import { useObjectifs } from '@/features/objectifs/api/use-objectifs';
import { useCreateObjectif } from '@/features/objectifs/api/use-create-objectif';
import { useUpdateObjectifStatut } from '@/features/objectifs/api/use-update-objectif-statut';
import { useWeeklyTracking } from '@/features/objectifs/api/use-weekly-tracking';
import { useBadges } from '@/features/gamification/api/use-badges';
import { useRappels } from '@/features/gamification/api/use-rappels';
import { useCreateRappel } from '@/features/gamification/api/use-create-rappel';
import { useDeleteRappel } from '@/features/gamification/api/use-delete-rappel';
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
  const { data: weeklyTracking } = useWeeklyTracking(activeSpaceId);
  const { data: badges } = useBadges();
  const { data: rappels } = useRappels();
  const createRappel = useCreateRappel();
  const deleteRappel = useDeleteRappel();
  const createObjectif = useCreateObjectif();
  const updateStatut = useUpdateObjectifStatut(activeSpaceId ?? '');
  const [titre, setTitre] = useState('');
  const [dateEcheance, setDateEcheance] = useState('');
  const [rappelMessage, setRappelMessage] = useState('');
  const [rappelDate, setRappelDate] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!activeSpaceId || !titre.trim()) return;
    createObjectif.mutate(
      {
        spaceId: activeSpaceId,
        titre: titre.trim(),
        ...(dateEcheance ? { dateEcheance } : {}),
      },
      {
        onSuccess: () => {
          setTitre('');
          setDateEcheance('');
        },
      },
    );
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
              <div className="min-w-0 flex-1 pr-3">
                <span className="text-sm font-medium text-encre">{o.titre}</span>
                {o.dateEcheance && (
                  <p className="font-mono text-[0.68rem] text-encre-muted">
                    Date limite : {new Date(o.dateEcheance).toLocaleDateString('fr-FR')}
                  </p>
                )}
              </div>
              <div className="flex flex-none items-center gap-2">
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
          {objectifs?.length === 0 && (
            <p className="text-sm text-encre-muted">Aucun objectif défini pour cet espace.</p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-2 sm:flex-row">
          <Input
            placeholder="Nouvel objectif"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            className="flex-1"
            required
          />
          <Input
            type="date"
            title="Date limite (optionnelle)"
            value={dateEcheance}
            onChange={(e) => setDateEcheance(e.target.value)}
            className="w-full sm:w-36 font-mono text-xs"
          />
          <Button type="submit" disabled={!activeSpaceId || createObjectif.isPending || !titre.trim()}>
            {createObjectif.isPending ? 'Ajout…' : 'Ajouter'}
          </Button>
        </form>
      </div>

      <div>
        <h2 className="mb-4 font-display text-lg font-semibold text-encre">Badges</h2>
        <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
          {badges?.map((b) => (
            <div
              key={b.id}
              className={`flex items-start gap-3 rounded-fiche border p-3 ${
                b.obtenu
                  ? 'border-papier-border bg-papier-carte'
                  : 'border-dashed border-papier-border bg-papier-carte/50 opacity-75'
              }`}
            >
              <div
                className={`flex size-12 flex-none items-center justify-center rounded-full text-center font-mono text-[0.6rem] uppercase leading-tight ${
                  b.obtenu
                    ? 'bg-tag-sciences font-bold text-white'
                    : 'border-2 border-dashed border-papier-border text-encre-muted'
                }`}
              >
                {b.obtenu ? '★' : '🔒'}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5">
                  <p className="text-sm font-medium text-encre">{b.nom}</p>
                  {b.obtenu && (
                    <span className="rounded bg-succes/20 px-1.5 py-0.2 font-mono text-[0.6rem] text-succes">
                      Débloqué
                    </span>
                  )}
                </div>
                <p className="mt-0.5 text-xs text-encre-muted">{b.description}</p>
              </div>
            </div>
          ))}
          {badges?.length === 0 && (
            <p className="text-sm text-encre-muted">Aucun badge disponible pour le moment.</p>
          )}
        </div>

        <h2 className="mb-3 font-display text-lg font-semibold text-encre">Rappels</h2>
        <div className="mb-4 flex flex-col gap-2">
          {rappels?.map((r) => (
            <div key={r.id} className="flex items-center justify-between rounded-fiche border border-papier-border bg-papier-carte px-3 py-2 text-sm">
              <span>{r.message}</span>
              <div className="flex flex-none items-center gap-2">
                <StatutBadge variant={r.envoye ? 'secondary' : 'attention'}>
                  {new Date(r.prevuLe).toLocaleDateString('fr-FR')}
                </StatutBadge>
                {!r.envoye && (
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={deleteRappel.isPending}
                    onClick={() => deleteRappel.mutate(r.id)}
                  >
                    Supprimer
                  </Button>
                )}
              </div>
            </div>
          ))}
          {rappels?.length === 0 && (
            <p className="text-sm text-encre-muted">Aucun rappel programmé.</p>
          )}
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!rappelMessage.trim() || !rappelDate) return;
            createRappel.mutate(
              { message: rappelMessage.trim(), prevuLe: rappelDate },
              { onSuccess: () => { setRappelMessage(''); setRappelDate(''); } },
            );
          }}
          className="flex flex-col gap-2 sm:flex-row"
        >
          <Input
            placeholder="Message du rappel"
            value={rappelMessage}
            onChange={(e) => setRappelMessage(e.target.value)}
            className="flex-1"
            required
          />
          <Input
            type="date"
            title="Date du rappel"
            value={rappelDate}
            onChange={(e) => setRappelDate(e.target.value)}
            className="w-full sm:w-36 font-mono text-xs"
            required
          />
          <Button type="submit" disabled={createRappel.isPending || !rappelMessage.trim() || !rappelDate}>
            {createRappel.isPending ? 'Ajout…' : 'Programmer'}
          </Button>
        </form>

        <div className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-display text-sm font-semibold text-encre">Suivi hebdomadaire</h2>
            <span className="rounded bg-secondary px-2 py-0.5 font-mono text-[0.62rem] text-encre-muted">
              Bientôt disponible
            </span>
          </div>
          {weeklyTracking ? (
            <div className="flex items-center justify-between text-xs">
              <span>Semaine : {weeklyTracking.semaine}</span>
              <span className="font-mono">{weeklyTracking.nbObjectifsAtteints} atteint(s)</span>
            </div>
          ) : (
            <p className="text-xs text-encre-muted">
              Le récapitulatif hebdomadaire et le taux de complétion régulier seront calculés automatiquement dans une prochaine mise à jour.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
