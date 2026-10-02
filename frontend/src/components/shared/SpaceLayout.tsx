import { Link, Outlet } from '@tanstack/react-router';

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

  return (
    <div className="flex h-full flex-col">
      <div className="px-4 pt-6 sm:px-6 lg:px-8">
        <Link to={backTo} className="rounded-sm font-mono text-xs text-encre-muted hover:text-encre focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          {backLabel}
        </Link>
        <div className="mt-2 flex min-w-0 items-center gap-2.5">
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

      <div className="min-h-0 flex-1 overflow-y-auto">
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
