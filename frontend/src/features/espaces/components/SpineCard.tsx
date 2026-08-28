import { Link } from '@tanstack/react-router';

import { useConversations } from '@/features/chat/api/use-conversations';
import { cn } from '@/lib/utils';
import { getTagColorClass } from '../lib/get-tag-color';
import type { Space } from '../types';

interface SpineCardProps {
  space: Space;
}

/**
 * Le "dos de reliure" de l'étagère (cartographie UI, Layout App Étudiant).
 * Réutilisé tel quel partout où un espace doit s'afficher sous cette forme —
 * ne pas redessiner une variante ad hoc ailleurs.
 */
export function SpineCard({ space }: SpineCardProps) {
  const { data: conversations } = useConversations(space.id);
  const latestActivity = conversations?.[0]?.updatedAt ?? space.createdAt;

  return (
    <Link
      to="/espaces/$spaceId"
      params={{ spaceId: space.id }}
      title={`Dernière activité : ${new Date(latestActivity).toLocaleDateString('fr-FR')}`}
      className={cn(
        'group relative flex h-56 w-20 flex-none flex-col justify-end overflow-hidden rounded-fiche p-3 text-white shadow-sm transition-transform hover:-translate-y-1 hover:shadow-md',
        getTagColorClass(space.subjectTag),
      )}
    >
      <span className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
      <div className="absolute inset-x-2 top-2 flex items-center justify-between font-mono text-[0.58rem] opacity-85">
        <span>{new Date(latestActivity).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })}</span>
        {space.documentCount !== undefined && (
          <span className="rounded bg-white/20 px-1 py-0.2">
            {space.documentCount}
          </span>
        )}
      </div>
      <span
        className="relative font-display text-sm font-medium leading-tight"
        style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
      >
        {space.name}
      </span>
    </Link>
  );
}
