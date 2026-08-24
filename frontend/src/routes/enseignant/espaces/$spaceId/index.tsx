import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { useEspace } from '@/features/espaces/api/use-espace';
import { fichesForSpaceQueryOptions, useFichesForSpace } from '@/features/fiches/api/use-fiches-for-space';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(fichesForSpaceQueryOptions(params.spaceId)),
  component: FichesEspaceEnseignant,
});

/**
 * Fiches d'un espace, vue ENSEIGNANT : toutes les fiches de tous les étudiants
 * de l'espace. Le détail (avec verdict) est sur la page fiche dédiée.
 */
function FichesEspaceEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/' });
  const { data: fiches } = useFichesForSpace(spaceId);
  const { data: espace } = useEspace(spaceId);

  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">
        {espace?.name ?? 'Espace'}
      </p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Fiches à valider</h1>

      {fiches?.length === 0 && (
        <p className="text-sm text-encre-muted">Aucune fiche générée dans cet espace.</p>
      )}

      <div className="flex flex-col gap-2">
        {fiches?.map((fiche) => (
          <Link
            key={fiche.id}
            to="/enseignant/espaces/$spaceId/fiches/$ficheId"
            params={{ spaceId, ficheId: fiche.id }}
            className="flex items-center gap-3 rounded-fiche border border-papier-border bg-papier-carte p-3 hover:bg-secondary"
          >
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-encre">{fiche.title}</p>
              <p className="font-mono text-[0.68rem] text-encre-muted">
                par {fiche.userId.slice(0, 8)}… · {new Date(fiche.updatedAt).toLocaleDateString('fr-FR')}
                {fiche.obsolete && <span className="ml-2 text-attention">obsolète</span>}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
