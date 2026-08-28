import { createFileRoute, Link } from '@tanstack/react-router';
import { useQueries } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { espacesQueryOptions, useEspaces } from '@/features/espaces/api/use-espaces';
import { conversationsQueryOptions } from '@/features/chat/api/use-conversations';
import { useFichesMine } from '@/features/fiches/api/use-fiches';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { JoinEspaceModal } from '@/features/espaces/components/JoinEspaceModal';
import { EtagereGrid } from '@/features/espaces/components/EtagereGrid';
import type { Conversation } from '@/features/chat/types';

export const Route = createFileRoute('/_app/')({
  // Précharge la liste des espaces AVANT que la page s'affiche — quand le
  // composant monte, useEspaces() dans EtagereGrid retombe direct sur du
  // cache déjà chaud, pas de spinner si la navigation s'est faite via <Link>.
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesQueryOptions),
  component: Etagere,
});

function Etagere() {
  const { data: espaces } = useEspaces();
  const { data: fiches } = useFichesMine();

  const conversationsQueries = useQueries({
    queries: (espaces ?? []).map((space) => ({
      ...conversationsQueryOptions(space.id),
      select: (convs: Conversation[]) => convs.map((c) => ({ ...c, spaceName: space.name })),
    })),
  });

  const allConversations = conversationsQueries.flatMap((q) => q.data ?? []);
  const latestConv = allConversations.sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  )[0];

  const latestFiche = (fiches ?? []).slice().sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  )[0];

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4 px-8 pt-7">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Mes espaces</p>
          <h1 className="font-display text-2xl font-semibold text-encre">L'étagère</h1>
        </div>
        <div className="flex items-center gap-2">
          <JoinEspaceModal trigger={<Button variant="outline">Rejoindre via code</Button>} />
          <CreateEspaceModal trigger={<Button>Créer un espace</Button>} />
        </div>
      </div>

      {(latestConv || latestFiche) && (
        <div className="mx-8 mt-6 rounded-fiche border border-papier-border bg-papier-carte p-4">
          <p className="mb-2 font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">
            Reprendre où j'en étais
          </p>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {latestConv && (
              <Link
                to="/espaces/$spaceId/chat"
                params={{ spaceId: latestConv.spaceId }}
                className="flex items-center gap-3 rounded-md border border-papier-border bg-background p-3 transition-colors hover:bg-secondary"
              >
                <span className="text-xl">💬</span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-encre">
                    {latestConv.title || 'Conversation en cours'}
                  </p>
                  <p className="font-mono text-[0.65rem] text-encre-muted">
                    {latestConv.spaceName} · {new Date(latestConv.updatedAt).toLocaleDateString('fr-FR')}
                  </p>
                </div>
              </Link>
            )}
            {latestFiche && (
              <Link
                to="/espaces/$spaceId/fiches/$ficheId"
                params={{ spaceId: latestFiche.spaceId, ficheId: latestFiche.id }}
                className="flex items-center gap-3 rounded-md border border-papier-border bg-background p-3 transition-colors hover:bg-secondary"
              >
                <span className="text-xl">📄</span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-encre">{latestFiche.title}</p>
                  <p className="font-mono text-[0.65rem] text-encre-muted">
                    Fiche révisée · {new Date(latestFiche.updatedAt).toLocaleDateString('fr-FR')}
                  </p>
                </div>
              </Link>
            )}
          </div>
        </div>
      )}

      <EtagereGrid />
    </div>
  );
}
