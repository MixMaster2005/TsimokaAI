import { createFileRoute, Link, Outlet, useParams } from '@tanstack/react-router';

import { espaceQueryOptions, useEspace } from '@/features/espaces/api/use-espace';

/**
 * ⚠️ Point ouvert de la cartographie UI : un étudiant créateur d'un espace
 * perso voit-il l'onglet Paramètres comme un enseignant sur le sien ?
 * Ici : visible dès que `space.userId === session.user.id` (créateur =
 * propriétaire, peu importe le rôle global) — cohérent avec l'hypothèse
 * retenue à l'époque, mais TODO confirmer explicitement.
 */
export const Route = createFileRoute('/_app/espaces/$spaceId')({
  loader: ({ context: { queryClient }, params }) => queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
  component: EspaceLayout,
});

const TABS = [
  { to: '/espaces/$spaceId/chat', label: 'Chat' },
  { to: '/espaces/$spaceId/fiches', label: 'Fiches' },
  { to: '/espaces/$spaceId/documents', label: 'Documents' },
  { to: '/espaces/$spaceId/membres', label: 'Membres' },
  { to: '/espaces/$spaceId/parametres', label: 'Paramètres' },
] as const;

function EspaceLayout() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId' });
  const { data: space } = useEspace(spaceId);

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
        {TABS.map((tab) => (
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
