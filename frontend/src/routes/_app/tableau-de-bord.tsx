import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { useEspaces } from '@/features/espaces/api/use-espaces';
import {
  useAllTauxReussite,
  useStudentDashboard,
} from '@/features/dashboard/api/use-student-dashboard';

export const Route = createFileRoute('/_app/tableau-de-bord')({
  component: TableauDeBord,
});

function TableauDeBord() {
  const { data: espaces } = useEspaces();
  const [spaceId, setSpaceId] = useState<string | null>(null);
  const activeSpaceId = spaceId ?? espaces?.[0]?.id ?? null;

  const { data: dashboard } = useStudentDashboard(activeSpaceId ?? '');
  const tauxParMatiere = useAllTauxReussite(espaces);

  return (
    <div className="p-8">
      <div className="mb-4 flex items-start justify-between">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Tableau de bord</p>
          <h1 className="font-display text-2xl font-semibold text-encre">Ma progression</h1>
        </div>
        {espaces && espaces.length > 1 && (
          <select
            value={activeSpaceId ?? ''}
            onChange={(e) => setSpaceId(e.target.value)}
            className="rounded-md border border-papier-border bg-papier-carte px-2 py-1.5 text-sm"
          >
            {espaces.map((e) => (
              <option key={e.id} value={e.id}>
                {e.name}
              </option>
            ))}
          </select>
        )}
      </div>

      {dashboard && (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          <div className="rounded-fiche border border-papier-border bg-papier-carte p-5">
            <h3 className="mb-3 font-display text-sm font-semibold text-encre">Progression</h3>
            <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">Maîtrisées</p>
            <p className="mb-3 text-sm text-encre">{dashboard.notionsMaitrisees.join(', ') || '—'}</p>
            <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-attention">Fragiles</p>
            <p className="text-sm text-encre opacity-60">{dashboard.notionsFaibles.join(', ') || '—'}</p>
          </div>

          <div className="rounded-fiche border border-papier-border bg-papier-carte p-5">
            <h3 className="mb-3 font-display text-sm font-semibold text-encre">Recommandations de l'IA</h3>
            <div className="flex flex-col gap-3">
              {dashboard.recommandations.map((r) => (
                <div key={r.id} className="text-sm">
                  <span className="mr-2 rounded bg-tag-sciences px-2 py-0.5 font-mono text-[0.6rem] uppercase text-white">
                    {r.type.replace(/_/g, ' ')}
                  </span>
                  {r.contenu}
                </div>
              ))}
              {dashboard.recommandations.length === 0 && (
                <p className="text-sm text-encre-muted">Rien à signaler pour l'instant.</p>
              )}
            </div>
          </div>

          <div className="rounded-fiche border border-papier-border bg-papier-carte p-5 lg:col-span-2">
            <h3 className="mb-3 font-display text-sm font-semibold text-encre">Taux de réussite par matière</h3>
            <div className="flex flex-col gap-2">
              {tauxParMatiere.map((q) =>
                q.data ? (
                  <div key={q.data.space.id} className="flex items-center gap-3 text-sm">
                    <span className="w-32 flex-none truncate text-encre">{q.data.space.name}</span>
                    <div className="h-1.5 flex-1 rounded-full bg-papier-bg">
                      <div
                        className="h-full rounded-full bg-encre"
                        style={{ width: `${Math.round(q.data.tauxReussite * 100)}%` }}
                      />
                    </div>
                    <span className="w-10 flex-none text-right font-mono text-xs text-encre-muted">
                      {Math.round(q.data.tauxReussite * 100)}%
                    </span>
                  </div>
                ) : null,
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
