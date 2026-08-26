import { createFileRoute, Link } from '@tanstack/react-router';

import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';

/**
 * Tableau de bord ENSEIGNANT — vue de supervision : tous les espaces de la
 * plateforme (GET /api/v1/spaces/all), point d'entrée vers les fiches à valider.
 *
 * Limite : les agrégats "chapitres difficiles avec densité d'encre" du contrat
 * de design nécessitent des endpoints analytiques non implémentés côté
 * analytics-service — voir README frontend, section Limitations connues.
 */
export const Route = createFileRoute('/enseignant/')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { data: espaces } = useEspacesAll();

  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue enseignant</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Espaces de cours</h1>

      {espaces?.length === 0 && (
        <p className="text-sm text-encre-muted">Aucun espace créé pour l'instant.</p>
      )}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-3">
        {espaces?.map((espace) => (
          <Link
            key={espace.id}
            to="/enseignant/espaces/$spaceId"
            params={{ spaceId: espace.id }}
            className="flex items-center gap-3 rounded-fiche border border-papier-border bg-papier-carte p-4 hover:bg-secondary"
          >
            <span className={cn('w-1.5 self-stretch rounded-full', getTagColorClass(espace.subjectTag))} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-encre">{espace.name}</p>
              <p className="font-mono text-[0.68rem] text-encre-muted">
                {espace.subjectTag ?? 'sans tag'}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
