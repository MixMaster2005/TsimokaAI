import { createFileRoute, Link, Outlet, useParams } from '@tanstack/react-router';

import { espaceQueryOptions, useEspace } from '@/features/espaces/api/use-espace';
import { useSession } from '@/features/auth/api/use-session';

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

const TABS_BASE = [
  { to: '/espaces/$spaceId/chat', label: 'Chat' },
  { to: '/espaces/$spaceId/fiches', label: 'Fiches' },
  { to: '/espaces/$spaceId/documents', label: 'Documents' },
  { to: '/espaces/$spaceId/membres', label: 'Membres' },
] as const;

const TAB_PARAMETRES = { to: '/espaces/$spaceId/parametres', label: 'Paramètres' } as const;

function EspaceLayout() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId' });
  const { data: space } = useEspace(spaceId);
  const { data: session } = useSession();

  const isOwner = space?.userId === session?.id;
  const tabs = isOwner ? [...TABS_BASE, TAB_PARAMETRES] : TABS_BASE;

  return (
    <div className="flex h-full flex-col">
      <div className="px-6 pt-5">
        <Link to="/" className="font-mono text-xs text-muted-foreground hover:text-foreground">
          ← Mes espaces
        </Link>
        <h1 className="mt-2 font-display text-xl font-semibold text-foreground">{space?.name}</h1>
        {space?.description && <p className="mt-1 text-sm text-muted-foreground">{space.description}</p>}
      </div>

      <nav className="mt-4 flex gap-1 border-b border-border px-6">
        {tabs.map((tab) => (
          <Link
            key={tab.to}
            to={tab.to}
            params={{ spaceId }}
            className="-mb-px border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground hover:text-foreground"
            activeProps={{ className: 'border-tag-sciences text-foreground' }}
          >
            {tab.label}
          </Link>
        ))}
      </nav>

      <div className="min-h-0 flex-1">
        <Outlet />
      </div>
    </div>
  );
}
