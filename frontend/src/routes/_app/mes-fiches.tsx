import { createFileRoute, Link } from '@tanstack/react-router';

import { fichesMineQueryOptions, useFichesMine } from '@/features/fiches/api/use-fiches';
import { useEspaces } from '@/features/espaces/api/use-espaces';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';

/**
 * Vue transverse "Mes fiches" : toutes les fiches de l'étudiant, tous espaces
 * confondus, plus récentes d'abord (tri fait côté back). Repose sur
 * GET /api/v1/fiches/mine — l'endpoint ajouté à FicheController pour lever le
 * blocage noté pendant le scaffolding (listMine exigeait un spaceId obligatoire).
 */
export const Route = createFileRoute('/_app/mes-fiches')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(fichesMineQueryOptions),
  component: MesFiches,
});

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
                <p className="truncate text-sm font-medium text-encre">{fiche.title}</p>
                <p className="font-mono text-[0.68rem] text-encre-muted">
                  {/* L'espace d'origine est l'info clé ici : on vient justement de tous les espaces */}
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
