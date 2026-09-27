import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { AnnotationsSection } from '@/features/fiches/components/AnnotationsSection';
import { FicheCard } from '@/features/fiches/components/FicheCard';
import { ValidationSection } from '@/features/fiches/components/ValidationSection';
import { ficheQueryOptions, useFiche } from '@/features/fiches/api/use-fiche';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/fiches/$ficheId')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(ficheQueryOptions(params.ficheId)),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]),
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
  const { data: fiche, isLoading } = useFiche(ficheId);
  const { data: espace } = useEspace(spaceId);

  if (isLoading) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col gap-3 p-4 sm:p-6">
        <div role="status" aria-live="polite" aria-busy="true" className="flex flex-col gap-3">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-48 w-full" />
          <span className="sr-only">Chargement de la fiche…</span>
        </div>
      </div>
    );
  }

  if (!fiche) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-start gap-3 p-4 sm:p-6">
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Fiche</p>
        <h1 className="font-display text-xl font-semibold text-encre">Fiche introuvable</h1>
        <p className="text-sm text-encre-muted">
          Cette fiche n'existe pas ou n'est plus disponible. Retourne à la liste des fiches.
        </p>
        <Link to="/enseignant/espaces/$spaceId/fiches" params={{ spaceId }}>
          <Button variant="outline">Retour aux fiches</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-4 sm:p-6">
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
