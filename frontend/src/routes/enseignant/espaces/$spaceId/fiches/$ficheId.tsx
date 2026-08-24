import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { useEspace } from '@/features/espaces/api/use-espace';
import { AnnotationsSection } from '@/features/fiches/components/AnnotationsSection';
import { FicheCard } from '@/features/fiches/components/FicheCard';
import { ValidationSection } from '@/features/fiches/components/ValidationSection';
import { ficheQueryOptions, useFiche } from '@/features/fiches/api/use-fiche';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/fiches/$ficheId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(ficheQueryOptions(params.ficheId)),
  component: FicheDetailEnseignant,
});

/**
 * Détail d'une fiche côté ENSEIGNANT : contenu + verdict de validation.
 * Réutilise les mêmes composants que la vue étudiant (FicheCard,
 * ValidationSection) — seul le chrome et le chemin changent ; la modale de
 * partage n'a pas de sens ici (le back réserve le partage au propriétaire).
 */
function FicheDetailEnseignant() {
  const { spaceId, ficheId } = useParams({ from: '/enseignant/espaces/$spaceId/fiches/$ficheId' });
  const { data: fiche } = useFiche(ficheId);
  const { data: espace } = useEspace(spaceId);

  if (!fiche) return null;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-6">
      <div>
        <Link
          to="/enseignant/espaces/$spaceId"
          params={{ spaceId }}
          className="text-xs text-encre-muted hover:text-encre"
        >
          ← {espace?.name ?? 'Retour aux fiches'}
        </Link>
        <div className="mt-3">
          <FicheCard fiche={fiche} subjectTag={espace?.subjectTag} />
        </div>
      </div>

      <AnnotationsSection ficheId={ficheId} />
      <ValidationSection ficheId={ficheId} />
    </div>
  );
}
