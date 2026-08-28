import { useState } from 'react';
import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { fichesBySpaceQueryOptions, useFiches } from '@/features/fiches/api/use-fiches';
import { useValidation } from '@/features/fiches/api/validation-query-options';
import { GenerateFicheModal } from '@/features/fiches/components/GenerateFicheModal';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';

export const Route = createFileRoute('/_app/espaces/$spaceId/fiches/')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(fichesBySpaceQueryOptions(params.spaceId)),
  component: FichesEspace,
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

function FichesEspace() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/fiches/' });
  const { data: fiches } = useFiches(spaceId);
  const { data: space } = useEspace(spaceId);
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-foreground">Fiches</h2>
        <Button onClick={() => setModalOpen(true)}>Générer une fiche</Button>
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
              <div className="flex items-center gap-2">
                <p className="truncate text-sm font-medium text-foreground">{fiche.title}</p>
                <FicheValidationBadge ficheId={fiche.id} />
              </div>
              <p className="font-mono text-[0.68rem] text-muted-foreground">
                {new Date(fiche.updatedAt).toLocaleDateString('fr-FR')}
                {fiche.obsolete && <span className="ml-2 text-attention">obsolète</span>}
              </p>
            </div>
          </Link>
        ))}
      </div>

      <GenerateFicheModal open={modalOpen} onOpenChange={setModalOpen} spaceId={spaceId} />
    </div>
  );
}
