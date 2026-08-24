import { Link } from '@tanstack/react-router';

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
  return (
    <Link
      to="/espaces/$spaceId"
      params={{ spaceId: space.id }}
      className={cn(
        'group relative flex h-56 w-20 flex-none flex-col justify-end overflow-hidden rounded-fiche p-3 text-white shadow-sm transition-transform hover:-translate-y-1 hover:shadow-md',
        getTagColorClass(space.subjectTag),
      )}
    >
      <span className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
      {space.documentCount !== undefined && (
        <span className="absolute right-2 top-2 rounded bg-white/20 px-1.5 py-0.5 font-mono text-[0.6rem]">
          {space.documentCount}
        </span>
      )}
      <span
        className="relative font-display text-sm font-medium leading-tight"
        style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
      >
        {space.name}
      </span>
    </Link>
  );
}
