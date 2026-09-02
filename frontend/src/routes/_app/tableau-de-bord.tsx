import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { useEspaces } from '@/features/espaces/api/use-espaces';
import {
  useAllTauxReussite,
  useStudentDashboard,
} from '@/features/dashboard/api/use-student-dashboard';
import { useSessionHistory } from '@/features/dashboard/api/use-session-history';

export const Route = createFileRoute('/_app/tableau-de-bord')({
  component: TableauDeBord,
});

function TableauDeBord() {
  const { data: espaces } = useEspaces();
  const [spaceId, setSpaceId] = useState<string | null>(null);
  const activeSpaceId = spaceId ?? espaces?.[0]?.id ?? null;

  const { data: dashboard } = useStudentDashboard(activeSpaceId ?? '');
  const { data: sessionHistory } = useSessionHistory(activeSpaceId);
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
            <h3 className="mb-3 font-display text-sm font-semibold text-encre">Activité</h3>
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <p className="text-sm text-encre">Questions posées</p>
                <p className="font-mono text-lg font-semibold text-encre">{dashboard.nbQuestionsPosees}</p>
              </div>
              <div className="flex items-center justify-between">
                <p className="text-sm text-encre">Fiches générées</p>
                <p className="font-mono text-lg font-semibold text-encre">{dashboard.nbFichesGenerees}</p>
              </div>
            </div>
          </div>

          <div className="rounded-fiche border border-papier-border bg-papier-carte p-5">
            <h3 className="mb-3 font-display text-sm font-semibold text-encre">Progression des notions</h3>
            <div className="flex flex-col gap-3">
              <div>
                <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-succes">Maîtrisées</p>
                <p className="text-sm text-encre">{dashboard.notionsMaitrisees.join(', ') || '—'}</p>
              </div>
              <div>
                <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-attention">Fragiles / À revoir</p>
                <p className="text-sm text-encre opacity-75">{dashboard.notionsFaibles.join(', ') || '—'}</p>
              </div>
            </div>
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
              {tauxParMatiere.map((q) => {
                if (!q.data) return null;
                const percent = Math.round(q.data.tauxReussite * 100);
                const isWeak = percent < 50;
                return (
                  <div key={q.data.space.id} className="flex items-center gap-3 text-sm">
                    <span className="w-32 flex-none truncate text-encre">{q.data.space.name}</span>
                    <div className="h-1.5 flex-1 rounded-full bg-papier-bg">
                      <div
                        className={`h-full rounded-full ${isWeak ? 'bg-attention' : 'bg-encre'}`}
                        style={{ width: `${Math.max(percent, 2)}%` }}
                      />
                    </div>
                    <span
                      className={`w-10 flex-none text-right font-mono text-xs ${
                        isWeak ? 'font-semibold text-attention' : 'text-encre-muted'
                      }`}
                    >
                      {percent}%
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="rounded-fiche border border-papier-border bg-papier-carte p-5 lg:col-span-2">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="font-display text-sm font-semibold text-encre">Historique des sessions de révision</h3>
              <span className="rounded bg-secondary px-2 py-0.5 font-mono text-[0.62rem] text-encre-muted">
                Bientôt disponible
              </span>
            </div>
            {sessionHistory && sessionHistory.length > 0 ? (
              <div className="flex flex-col gap-2">
                {sessionHistory.map((s) => (
                  <div key={s.id} className="flex items-center justify-between text-sm">
                    <span>{s.titre}</span>
                    <span className="font-mono text-xs text-encre-muted">
                      {s.dureeMinutes} min · {new Date(s.dateSession).toLocaleDateString('fr-FR')}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-encre-muted">
                Le journal détaillé des sessions d'entraînement sera synchronisé automatiquement dans une prochaine mise à jour.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
