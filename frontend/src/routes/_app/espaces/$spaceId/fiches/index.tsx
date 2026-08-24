import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { fichesBySpaceQueryOptions, useFiches } from '@/features/fiches/api/use-fiches';
import { useGenerateFiche } from '@/features/fiches/api/use-generate-fiche';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';

export const Route = createFileRoute('/_app/espaces/$spaceId/fiches/')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(fichesBySpaceQueryOptions(params.spaceId)),
  component: FichesEspace,
});

function FichesEspace() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/fiches/' });
  const { data: fiches } = useFiches(spaceId);
  const { data: space } = useEspace(spaceId);
  const generateFiche = useGenerateFiche();

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-foreground">Fiches</h2>
        <Button
          onClick={() => generateFiche.mutate({ spaceId })}
          disabled={generateFiche.isPending}
        >
          {generateFiche.isPending ? 'Génération…' : 'Générer une fiche'}
        </Button>
      </div>

      {fiches?.length === 0 && (
        <p className="text-sm text-muted-foreground">Aucune fiche générée pour l'instant dans cet espace.</p>
      )}

      <div className="flex flex-col gap-2">
        {fiches?.map((fiche) => (
          <Link
            key={fiche.id}
            to="/espaces/$spaceId/fiches/$ficheId"
            params={{ spaceId, ficheId: fiche.id }}
            className="flex items-center gap-3 overflow-hidden rounded-fiche border border-border bg-card p-3 hover:bg-secondary"
          >
            <span className={cn('w-1.5 self-stretch rounded-full', getTagColorClass(space?.subjectTag))} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">{fiche.title}</p>
              <p className="font-mono text-[0.68rem] text-muted-foreground">
                {new Date(fiche.updatedAt).toLocaleDateString('fr-FR')}
                {fiche.obsolete && <span className="ml-2 text-attention">obsolète</span>}
              </p>
            </div>
          </Link>
        ))}
      </div>

      {/* GenerateFicheModal (choix du périmètre : corpus entier / documents / thème)
          reste à écrire — pour l'instant le bouton génère sur tout le corpus de
          l'espace, cf. GenerateFicheRequest.documentIds optionnel côté back. */}
    </div>
  );
}
