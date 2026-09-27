import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useSession } from '@/features/auth/api/use-session';
import { useMembres } from '@/features/espaces/api/use-membres';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { useGroupes } from '@/features/groupes/api/use-groupes';
import { AnnotationsSection } from '@/features/fiches/components/AnnotationsSection';
import { ShareFicheModal } from '@/features/fiches/components/ShareFicheModal';
import { ValidationSection } from '@/features/fiches/components/ValidationSection';
import { FicheCard } from '@/features/fiches/components/FicheCard';
import { ficheQueryOptions, useFiche } from '@/features/fiches/api/use-fiche';

export const Route = createFileRoute('/_app/espaces/$spaceId/fiches/$ficheId')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(ficheQueryOptions(params.ficheId)),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]),
  component: FicheDetail,
});

function FicheDetail() {
  const { spaceId, ficheId } = useParams({ from: '/_app/espaces/$spaceId/fiches/$ficheId' });
  const { data: fiche, isLoading } = useFiche(ficheId);
  const { data: space } = useEspace(spaceId);
  const { data: session } = useSession();

  // Cibles de partage connues localement : groupes de l'espace + membres extérieurs.
  // Les query options retombent sur le cache déjà rempli par la page Membres.
  const { data: groupes } = useGroupes(spaceId);
  const { data: membres } = useMembres(spaceId);

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
        <Link to="/espaces/$spaceId/fiches" params={{ spaceId }}>
          <Button variant="outline">Retour aux fiches</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-4 sm:p-6">
      <div>
        <FicheCard fiche={fiche} subjectTag={space?.subjectTag} />
        {/* Partage : réservé au propriétaire de la fiche côté back (403 sinon).
            L'enseignant n'a pas à partager les fiches de ses étudiants. */}
        {session?.role !== 'ENSEIGNANT' && (
          <div className="mt-3">
            <ShareFicheModal
              ficheId={ficheId}
              groupes={(groupes ?? []).map((g) => ({ id: g.id, label: g.nom }))}
              membres={(membres ?? []).map((m) => ({ id: m.userId, label: `Membre ${m.userId.slice(0, 8)}…` }))}
              trigger={<Button variant="outline" size="sm">Partager</Button>}
            />
          </div>
        )}
      </div>

      <AnnotationsSection ficheId={ficheId} />
      <ValidationSection ficheId={ficheId} />
    </div>
  );
}
