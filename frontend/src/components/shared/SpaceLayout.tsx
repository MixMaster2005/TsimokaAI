import { Link, Outlet } from '@tanstack/react-router';

import { Badge } from '@/components/ui/badge';
import { useEspace } from '@/features/espaces/api/use-espace';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { useSession } from '@/features/auth/api/use-session';
import { cn } from '@/lib/utils';

interface SpaceLayoutProps {
  spaceId: string;
  backTo: string;
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
      <div className="px-6 pt-5">
        <a href={backTo} className="font-mono text-xs text-encre-muted hover:text-encre">
          {backLabel}
        </a>
        <div className="mt-2 flex items-center gap-2.5">
          <h1 className="font-display text-xl font-semibold text-encre">{space?.name}</h1>
          {backTo === '/enseignant' && <Badge variant="secondary">Vue enseignant</Badge>}
          {space?.subjectTag && (
            <span
              className={cn(
                'inline-flex items-center rounded-sm px-2 py-0.5 font-mono text-[0.65rem] font-medium uppercase tracking-wide text-white',
                getTagColorClass(space.subjectTag),
              )}
            >
              {space.subjectTag}
            </span>
          )}
        </div>
        {space?.description && <p className="mt-1 text-sm text-encre-muted">{space.description}</p>}
      </div>

      <nav className="mt-4 flex gap-1 border-b border-papier-border px-6">
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
