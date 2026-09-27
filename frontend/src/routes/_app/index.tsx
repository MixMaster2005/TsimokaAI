import { createFileRoute, Link } from '@tanstack/react-router';
import { useQueries } from '@tanstack/react-query';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import { espacesQueryOptions, useEspaces } from '@/features/espaces/api/use-espaces';
import { useSession } from '@/features/auth/api/use-session';
import { conversationsQueryOptions } from '@/features/chat/api/use-conversations';
import { useFichesMine } from '@/features/fiches/api/use-fiches';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { JoinEspaceModal } from '@/features/espaces/components/JoinEspaceModal';
import { EtagereGrid } from '@/features/espaces/components/EtagereGrid';
import { FILTRES_ETAGERE_DEFAUT, type FiltresEtagere } from '@/features/espaces/lib/filtrer-espaces';
import type { Conversation } from '@/features/chat/types';

/**
 * Filtres étagère dans l'URL : partageables + survivent au refresh.
 * Clés courtes (q/tag/tri/owner), seules les valeurs non-défaut sont écrites.
 * Les valeurs invalides retombent sur le défaut (jamais d'écran cassé).
 */
const etagereSearchSchema = z.object({
  q: z.string().optional(),
  tag: z.string().optional(),
  tri: z.string().optional(),
  owner: z.string().optional(),
});

const TRI_VALEURS = ['recent', 'nom', 'activite'] as const;
const OWNER_VALEURS = ['tous', 'miens', 'autres'] as const;

export const Route = createFileRoute('/_app/')({
  validateSearch: etagereSearchSchema,
  // Précharge la liste des espaces AVANT que la page s'affiche — quand le
  // composant monte, useEspaces() dans EtagereGrid retombe direct sur du
  // cache déjà chaud, pas de spinner si la navigation s'est faite via <Link>.
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesQueryOptions),
  component: Etagere,
});

function Etagere() {
  const { data: espaces, isLoading, isError, refetch } = useEspaces();
  const { data: session } = useSession();
  const { data: fiches } = useFichesMine();
  const navigate = Route.useNavigate();
  const search = Route.useSearch();

  const filtres: FiltresEtagere = {
    recherche: search.q ?? FILTRES_ETAGERE_DEFAUT.recherche,
    tag: search.tag ?? FILTRES_ETAGERE_DEFAUT.tag,
    tri: (TRI_VALEURS as readonly string[]).includes(search.tri ?? '')
      ? (search.tri as FiltresEtagere['tri'])
      : FILTRES_ETAGERE_DEFAUT.tri,
    owner: (OWNER_VALEURS as readonly string[]).includes(search.owner ?? '')
      ? (search.owner as FiltresEtagere['owner'])
      : FILTRES_ETAGERE_DEFAUT.owner,
  };

  function handleFiltresChange(v: FiltresEtagere) {
    navigate({
      search: {
        q: v.recherche || undefined,
        tag: v.tag || undefined,
        tri: v.tri !== FILTRES_ETAGERE_DEFAUT.tri ? v.tri : undefined,
        owner: v.owner !== FILTRES_ETAGERE_DEFAUT.owner ? v.owner : undefined,
      },
      replace: true,
    });
  }

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
      <div className="flex flex-wrap items-start justify-between gap-4 px-4 pt-5 sm:px-6 sm:pt-7 lg:px-8">
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
        <div className="mx-4 mt-6 sm:mx-6 lg:mx-8 rounded-fiche border border-papier-border bg-papier-carte p-4">
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

      <EtagereGrid
        espaces={espaces ?? []}
        isLoading={isLoading}
        isError={isError}
        currentUserId={session?.id}
        onRetry={() => refetch()}
        filtres={filtres}
        onFiltresChange={handleFiltresChange}
      />
    </div>
  );
}
