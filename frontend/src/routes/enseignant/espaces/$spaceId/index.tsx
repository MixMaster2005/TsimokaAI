import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { fichesForSpaceQueryOptions, useFichesForSpace } from '@/features/fiches/api/use-fiches-for-space';
import { useValidation } from '@/features/fiches/api/validation-query-options';
import { useValidateFiche } from '@/features/fiches/api/use-validate-fiche';
import { parseFicheContent, type Fiche } from '@/features/fiches/types';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(fichesForSpaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]),
  component: FichesEspaceEnseignant,
});

function FicheEnseignantRow({ fiche, spaceId }: { fiche: Fiche; spaceId: string }) {
  const content = parseFicheContent(fiche);
  const { data: validation } = useValidation(fiche.id);
  const validateFiche = useValidateFiche(fiche.id);

  return (
    <div className="rounded-fiche border border-papier-border bg-papier-carte p-4 transition-colors">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <Link
              to="/enseignant/espaces/$spaceId/fiches/$ficheId"
              params={{ spaceId, ficheId: fiche.id }}
              className="truncate font-display text-base font-semibold text-encre hover:underline"
            >
              {fiche.title}
            </Link>
            {validation?.statut === 'VALIDEE' && (
              <span className="rounded-sm border border-succes/40 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase text-succes">
                Validée
              </span>
            )}
            {validation?.statut === 'REJETEE' && (
              <span className="rounded-sm border border-attention/50 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase text-attention">
                À revoir
              </span>
            )}
            {fiche.obsolete && <span className="text-xs text-attention">(obsolète)</span>}
          </div>
          <p className="mt-0.5 font-mono text-[0.68rem] text-encre-muted">
            par {fiche.userId.slice(0, 8)}… · {new Date(fiche.updatedAt).toLocaleDateString('fr-FR')}
          </p>

          {content?.definition && (
            <p className="mt-2 line-clamp-2 text-xs leading-relaxed text-encre opacity-80">
              {content.definition}
            </p>
          )}
        </div>

        <div className="flex flex-none items-center gap-2">
          {(!validation || validation.statut !== 'VALIDEE') && (
            <Button
              size="sm"
              disabled={validateFiche.isPending}
              onClick={() => validateFiche.mutate({ statut: 'VALIDEE' })}
            >
              Valider
            </Button>
          )}
          <Button variant="outline" size="sm" asChild>
            <Link to="/enseignant/espaces/$spaceId/fiches/$ficheId" params={{ spaceId, ficheId: fiche.id }}>
              Détails & verdict
            </Link>
          </Button>
        </div>
      </div>
    </div>
  );
}

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
      <div className="mb-6 flex items-center gap-4">
        <h1 className="font-display text-2xl font-semibold text-encre">Fiches à valider</h1>
        <Link
          to="/enseignant/espaces/$spaceId/dashboard"
          params={{ spaceId }}
          className="text-sm text-encre-muted underline hover:text-encre"
        >
          Tableau de bord
        </Link>
      </div>

      {fiches?.length === 0 && (
        <p className="text-sm text-encre-muted">Aucune fiche générée dans cet espace.</p>
      )}

      <div className="flex flex-col gap-3">
        {fiches?.map((fiche) => (
          <FicheEnseignantRow key={fiche.id} fiche={fiche} spaceId={spaceId} />
        ))}
      </div>
    </div>
  );
}
