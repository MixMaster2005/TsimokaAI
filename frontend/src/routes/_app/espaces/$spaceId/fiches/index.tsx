import { useState } from 'react';
import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { fichesBySpaceQueryOptions, useFiches } from '@/features/fiches/api/use-fiches';
import { FicheValidationBadge } from '@/features/fiches/components/FicheValidationBadge';
import { GenerateFicheModal } from '@/features/fiches/components/GenerateFicheModal';
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
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-encre">Fiches</h2>
        <Button onClick={() => setModalOpen(true)}>Générer une fiche</Button>
      </div>

      {fiches?.length === 0 && (
        <div className="rounded-fiche border border-dashed border-papier-border bg-papier-carte p-6 text-sm text-encre-muted">
          Aucune fiche générée pour l'instant dans cet espace.
        </div>
      )}

      <div className="flex flex-col gap-2">
        {fiches?.map((fiche) => (
          <Link
            key={fiche.id}
            to="/espaces/$spaceId/fiches/$ficheId"
            params={{ spaceId, ficheId: fiche.id }}
            className="flex items-center gap-3 overflow-hidden rounded-fiche border border-papier-border bg-papier-carte p-3 hover:bg-secondary"
          >
            <span className={cn('w-1.5 self-stretch rounded-full', getTagColorClass(space?.subjectTag))} />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <p className="truncate text-sm font-medium text-encre">{fiche.title}</p>
                <FicheValidationBadge ficheId={fiche.id} />
              </div>
              <p className="font-mono text-[0.68rem] text-encre-muted">
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
