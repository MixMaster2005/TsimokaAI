import { createFileRoute, useParams } from '@tanstack/react-router';

import { ficheQueryOptions, useFiche } from '@/features/fiches/api/use-fiche';
import { FicheCard } from '@/features/fiches/components/FicheCard';
import { useEspace } from '@/features/espaces/api/use-espace';

export const Route = createFileRoute('/_app/espaces/$spaceId/fiches/$ficheId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(ficheQueryOptions(params.ficheId)),
  component: FicheDetail,
});

function FicheDetail() {
  const { spaceId, ficheId } = useParams({ from: '/_app/espaces/$spaceId/fiches/$ficheId' });
  const { data: fiche } = useFiche(ficheId);
  const { data: space } = useEspace(spaceId);

  if (!fiche) return null;

  return (
    <div className="mx-auto max-w-2xl p-6">
      <FicheCard fiche={fiche} subjectTag={space?.subjectTag} />
      {/* Actions à ajouter : partager (ShareFicheModal, POST /api/v1/fiches/{id}/share),
          annoter (CreateAnnotationRequest, feature à créer), statut de validation
          (ValidationController — enseignant uniquement). */}
    </div>
  );
}
