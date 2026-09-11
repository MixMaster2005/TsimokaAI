import { createFileRoute, useParams } from '@tanstack/react-router';

import { espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { SpaceLayout } from '@/components/shared/SpaceLayout';

/**
 * Layout D — Espace partagé entre les deux rôles.
 * L'onglet Paramètres n'est visible que pour le propriétaire (créateur) de
 * l'espace, indépendamment du rôle global (STUDENT/ENSEIGNANT) — cf. cartographie
 * UI D : "créateur = propriétaire de l'espace".
 */
export const Route = createFileRoute('/_app/espaces/$spaceId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
  component: EspaceLayout,
});

const TABS = [
  { to: '/espaces/$spaceId/chat', label: 'Chat' },
  { to: '/espaces/$spaceId/fiches', label: 'Fiches' },
  { to: '/espaces/$spaceId/documents', label: 'Documents' },
  { to: '/espaces/$spaceId/quiz', label: 'Quiz' },
  { to: '/espaces/$spaceId/membres', label: 'Membres' },
] as const;

const TAB_PARAMETRES = { to: '/espaces/$spaceId/parametres', label: 'Paramètres' } as const;

function EspaceLayout() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId' });
  return (
    <SpaceLayout spaceId={spaceId} backTo="/" tabs={TABS} tabParametres={TAB_PARAMETRES} />
  );
}
