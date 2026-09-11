import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { TeacherDashboardBlocks } from '@/components/shared/TeacherDashboardBlocks';
import { Button } from '@/components/ui/button';
import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { useEspacesStats } from '@/features/espaces/api/use-espaces-stats';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';

export const Route = createFileRoute('/enseignant/tableau-de-bord')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { data: espaces } = useEspacesAll();
  const { stats } = useEspacesStats(espaces);
  const [selectedSpaceId, setSelectedSpaceId] = useState<string | null>(null);

  const totalEtudiants = stats.reduce((sum, s) => sum + s.nbEtudiants, 0);
  const totalFiches = stats.reduce((sum, s) => sum + s.nbFiches, 0);

  const activeSpaceId = selectedSpaceId ?? espaces?.[0]?.id ?? null;

  return (
    <div className="p-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue enseignant</p>
          <h1 className="font-display text-2xl font-semibold text-encre">Tableau de bord</h1>
        </div>
        <CreateEspaceModal trigger={<Button>Créer un espace</Button>} />
      </div>

      {/* Compteurs agrégés */}
      <div className="mb-8 grid grid-cols-3 gap-4">
        <div className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <p className="font-mono text-2xl font-bold text-encre">{espaces?.length ?? 0}</p>
          <p className="font-mono text-[0.65rem] text-encre-muted">espaces gérés</p>
        </div>
        <div className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <p className="font-mono text-2xl font-bold text-encre">{totalEtudiants}</p>
          <p className="font-mono text-[0.65rem] text-encre-muted">étudiants inscrits</p>
        </div>
        <div className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <p className="font-mono text-2xl font-bold text-encre">{totalFiches}</p>
          <p className="font-mono text-[0.65rem] text-encre-muted">fiches générées</p>
        </div>
      </div>

      {espaces?.length === 0 && (
        <p className="text-sm text-encre-muted">Aucun espace créé pour l'instant.</p>
      )}

      {espaces && espaces.length > 0 && (
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <label htmlFor="espace-select" className="font-mono text-xs uppercase tracking-wide text-encre-muted">
            Espace piloté
          </label>
          <select
            id="espace-select"
            value={activeSpaceId ?? ''}
            onChange={(e) => setSelectedSpaceId(e.target.value)}
            className="rounded-fiche border border-papier-border bg-papier-carte px-3 py-2 text-sm text-encre"
          >
            {espaces.map((space) => (
              <option key={space.id} value={space.id}>
                {space.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {activeSpaceId && <TeacherDashboardBlocks spaceId={activeSpaceId} />}
    </div>
  );
}
