import { createFileRoute, useParams } from '@tanstack/react-router';

import { espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { SpaceLayout } from '@/components/shared/SpaceLayout';

export const Route = createFileRoute('/enseignant/espaces/$spaceId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
  component: EspaceEnseignantLayout,
});

const TABS = [
  { to: '/enseignant/espaces/$spaceId/dashboard', label: 'Pilotage' },
  { to: '/enseignant/espaces/$spaceId/fiches', label: 'Fiches' },
  { to: '/enseignant/espaces/$spaceId/chat', label: 'Chat' },
  { to: '/enseignant/espaces/$spaceId/documents', label: 'Documents' },
  { to: '/enseignant/espaces/$spaceId/quiz', label: 'Quiz' },
  { to: '/enseignant/espaces/$spaceId/membres', label: 'Membres' },
] as const;

const TAB_PARAMETRES = { to: '/enseignant/espaces/$spaceId/parametres', label: 'Paramètres' } as const;

function EspaceEnseignantLayout() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId' });
  return (
    <SpaceLayout spaceId={spaceId} backTo="/enseignant" backLabel="← Mes espaces" tabs={TABS} tabParametres={TAB_PARAMETRES} />
  );
}
