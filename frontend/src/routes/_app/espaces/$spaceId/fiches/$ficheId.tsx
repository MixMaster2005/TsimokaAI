import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { useSession } from '@/features/auth/api/use-session';
import { useMembres } from '@/features/espaces/api/use-membres';
import { useEspace } from '@/features/espaces/api/use-espace';
import { useGroupes } from '@/features/groupes/api/use-groupes';
import { AnnotationsSection } from '@/features/fiches/components/AnnotationsSection';
import { ShareFicheModal } from '@/features/fiches/components/ShareFicheModal';
import { ValidationSection } from '@/features/fiches/components/ValidationSection';
import { FicheCard } from '@/features/fiches/components/FicheCard';
import { ficheQueryOptions, useFiche } from '@/features/fiches/api/use-fiche';

export const Route = createFileRoute('/_app/espaces/$spaceId/fiches/$ficheId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(ficheQueryOptions(params.ficheId)),
  component: FicheDetail,
});

function FicheDetail() {
  const { spaceId, ficheId } = useParams({ from: '/_app/espaces/$spaceId/fiches/$ficheId' });
  const { data: fiche } = useFiche(ficheId);
  const { data: space } = useEspace(spaceId);
  const { data: session } = useSession();

  // Cibles de partage connues localement : groupes de l'espace + membres extérieurs.
  // Les query options retombent sur le cache déjà rempli par la page Membres.
  const { data: groupes } = useGroupes(spaceId);
  const { data: membres } = useMembres(spaceId);

  if (!fiche) return null;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-6">
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
