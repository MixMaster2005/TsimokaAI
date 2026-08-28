import { createFileRoute, Link } from '@tanstack/react-router';

import { fichesMineQueryOptions, useFichesMine } from '@/features/fiches/api/use-fiches';
import { useValidation } from '@/features/fiches/api/validation-query-options';
import { useEspaces } from '@/features/espaces/api/use-espaces';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';

/**
 * Vue transverse "Mes fiches" : toutes les fiches de l'étudiant, tous espaces
 * confondus, plus récentes d'abord (tri effectué côté back).
 * Endpoint : GET /api/v1/fiches/mine (FicheController.listAllMine).
 */
export const Route = createFileRoute('/_app/mes-fiches')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(fichesMineQueryOptions),
  component: MesFiches,
});

function FicheValidationBadge({ ficheId }: { ficheId: string }) {
  const { data: validation } = useValidation(ficheId);
  if (!validation) return null;

  if (validation.statut === 'VALIDEE') {
    return (
      <span className="inline-flex items-center rounded-sm border border-succes/40 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase tracking-wider text-succes">
        Validée
      </span>
    );
  }
  if (validation.statut === 'REJETEE') {
    return (
      <span className="inline-flex items-center rounded-sm border border-attention/50 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase tracking-wider text-attention">
        À revoir
      </span>
    );
  }
  return null;
}

function MesFiches() {
  const { data: fiches } = useFichesMine();
  const { data: espaces } = useEspaces();

  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue transverse</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Mes fiches</h1>

      {fiches?.length === 0 && (
        <div className="rounded-fiche border border-dashed border-papier-border bg-papier-carte p-6 text-sm text-encre-muted">
          Aucune fiche générée pour l'instant. Les fiches se créent depuis un espace de cours,
          onglet « Fiches ».
        </div>
      )}

      <div className="flex flex-col gap-2">
        {fiches?.map((fiche) => {
          const espace = espaces?.find((e) => e.id === fiche.spaceId);
          return (
            <Link
              key={fiche.id}
              to="/espaces/$spaceId/fiches/$ficheId"
              params={{ spaceId: fiche.spaceId, ficheId: fiche.id }}
              className="flex items-center gap-3 overflow-hidden rounded-fiche border border-papier-border bg-papier-carte p-3 hover:bg-secondary"
            >
              <span className={cn('w-1.5 self-stretch rounded-full', getTagColorClass(espace?.subjectTag))} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="truncate text-sm font-medium text-encre">{fiche.title}</p>
                  <FicheValidationBadge ficheId={fiche.id} />
                </div>
                <p className="font-mono text-[0.68rem] text-encre-muted">
                  {/* Espace d'origine : info discriminante dans une vue multi-espaces */}
                  {espace?.name ?? 'Espace supprimé'}
                  {' · '}
                  {new Date(fiche.updatedAt).toLocaleDateString('fr-FR')}
                  {fiche.obsolete && <span className="ml-2 text-attention">obsolète</span>}
                </p>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
