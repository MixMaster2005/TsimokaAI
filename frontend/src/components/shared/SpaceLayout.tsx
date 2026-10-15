import { useState } from 'react';
import { Link, Outlet, useLocation } from '@tanstack/react-router';
import { ChevronDown, ChevronUp } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { useSession } from '@/features/auth/api/use-session';
import { cn } from '@/lib/utils';

interface SpaceLayoutProps {
  spaceId: string;
  backTo: '/' | '/enseignant';
  backLabel?: string;
  tabs: ReadonlyArray<{ to: string; label: string }>;
  tabParametres?: { to: string; label: string };
}

export function SpaceLayout({ spaceId, backTo, backLabel = '← Mes espaces', tabs, tabParametres }: SpaceLayoutProps) {
  const { data: space } = useEspace(spaceId);
  const { data: session } = useSession();

  const isOwner = space?.userId === session?.id;
  const allTabs = isOwner && tabParametres ? [...tabs, tabParametres] : [...tabs];

  // Onglet chat : verrouillage viewport (hauteur exacte, pas de scroll de page).
  // La colonne ChatPage remplit la zone restante, seul le fil défile et l'input
  // reste fixé en bas. Les autres onglets gardent leur scroll de page habituel.
  const pathname = useLocation({ select: (s) => s.pathname });
  const isChat = pathname.endsWith('/chat');

  // Repli de l'en-tête + onglets (chat uniquement) : donne toute la hauteur au fil.
  // Persisté par espace pour retrouver son confort de lecture.
  const collapseKey = `tsimoka:space:${spaceId}:header-replie`;
  const [headerReplie, setHeaderReplie] = useState(() => {
    try {
      return localStorage.getItem(collapseKey) === '1';
    } catch {
      return false;
    }
  });
  function basculerHeader() {
    setHeaderReplie((replie) => {
      try {
        localStorage.setItem(collapseKey, replie ? '0' : '1');
      } catch {
        /* stockage indisponible : repli session uniquement */
      }
      return !replie;
    });
  }

  return (
    <div className={cn('flex flex-col', isChat ? 'h-svh' : 'h-full')}>
      {isChat && headerReplie ? (
        <div className="flex flex-none items-center gap-1.5 border-b border-papier-border bg-background px-4 py-2 sm:px-6">
          <button
            type="button"
            onClick={basculerHeader}
            aria-expanded="false"
            aria-label="Afficher l'en-tête et les onglets de l'espace"
            title="Afficher l'en-tête et les onglets"
            className="flex-none rounded-sm p-1.5 text-encre hover:bg-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <ChevronDown className="size-4" aria-hidden="true" />
          </button>
          <Link to={backTo} className="flex-none rounded-sm font-mono text-xs text-encre-muted hover:text-encre focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            {backLabel}
          </Link>
          <span className="min-w-0 flex-1 truncate text-sm font-medium text-encre" title={space?.name}>
            {space?.name ?? 'Espace'}
          </span>
        </div>
      ) : (
      <>
      <div className="px-4 pt-6 sm:px-6 lg:px-8">
        <Link to={backTo} className="rounded-sm font-mono text-xs text-encre-muted hover:text-encre focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          {backLabel}
        </Link>
        <div className="mt-2 flex min-w-0 items-center gap-2.5">
          {isChat && (
            <button
              type="button"
              onClick={basculerHeader}
              aria-expanded="true"
              aria-label="Masquer l'en-tête et les onglets de l'espace"
              title="Masquer pour agrandir le chat"
              className="flex-none rounded-sm p-1.5 text-encre-muted hover:bg-secondary hover:text-encre focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <ChevronUp className="size-4" aria-hidden="true" />
            </button>
          )}
          {space ? (
            <h1 className="min-w-0 flex-1 truncate font-display text-2xl font-semibold text-encre" title={space.name}>
              {space.name}
            </h1>
          ) : (
            <div role="status" aria-live="polite" aria-busy="true" className="min-w-0 flex-1">
              <Skeleton className="h-7 w-1/3" />
              <span className="sr-only">Chargement de l'espace…</span>
            </div>
          )}
          {backTo === '/enseignant' && <Badge variant="secondary">Vue enseignant</Badge>}
          {space?.subjectTag && (
            <span
              className={cn(
                'inline-flex flex-none items-center rounded-sm px-2 py-0.5 font-mono text-[0.65rem] font-medium uppercase tracking-wide text-white',
                getTagColorClass(space.subjectTag),
              )}
            >
              {space.subjectTag}
            </span>
          )}
        </div>
        {space?.description && <p className="mt-1 line-clamp-2 text-pretty text-sm text-encre-muted">{space.description}</p>}
      </div>

      <nav aria-label="Onglets espace" className="mt-4 flex gap-1 overflow-x-auto whitespace-nowrap border-b border-papier-border px-4 sm:px-6 lg:px-8">
        {allTabs.map((tab) => (
          <TabLink key={tab.to} tab={tab} spaceId={spaceId} />
        ))}
      </nav>
      </>
      )}

      <div className={cn('min-h-0 flex-1', isChat ? 'overflow-hidden' : 'overflow-y-auto')}>
        <Outlet />
      </div>
    </div>
  );
}

function TabLink({ tab, spaceId }: { tab: { to: string; label: string }; spaceId: string }) {
  return (
    <Link
      to={tab.to}
      params={{ spaceId }}
      className="-mb-px border-b-2 border-transparent px-3 py-2 text-sm font-medium text-encre-muted hover:text-encre"
      activeProps={{ className: 'border-tag-sciences text-encre' }}
    >
      {tab.label}
    </Link>
  );
}
