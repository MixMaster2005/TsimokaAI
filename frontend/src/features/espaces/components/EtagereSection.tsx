import { SpineCard } from './SpineCard';
import type { Space } from '../types';

interface EtagereSectionProps {
  titre: string;
  description?: string;
  espaces: Space[];
  emptyMessage: string;
  basePath: 'etudiant' | 'enseignant';
}

/**
 * Une planche de l'étagère : en-tête + rangée horizontale de dos de reliure
 * (SpineCard) + planche en bois. Ne charge aucune donnée elle-même :
 * les SpineCard gèrent déjà leurs conversations, pas de N+1 supplémentaire ici.
 */
export function EtagereSection({
  titre,
  description,
  espaces,
  emptyMessage,
  basePath,
}: EtagereSectionProps) {
  return (
    <section aria-label={titre} className="px-8 py-4">
      <header className="flex items-baseline justify-between gap-4">
        <div>
          <h2 className="font-display text-lg font-semibold text-encre">{titre}</h2>
          {description && <p className="mt-0.5 text-sm text-encre-muted">{description}</p>}
        </div>
        <span className="flex-none font-mono text-xs text-encre-muted">
          {espaces.length} espace{espaces.length > 1 ? 's' : ''}
        </span>
      </header>

      {espaces.length === 0 ? (
        <p className="py-6 text-sm text-encre-muted">{emptyMessage}</p>
      ) : (
        <div>
          <div className="flex gap-4 overflow-x-auto py-5">
            {espaces.map((space) => (
              <SpineCard key={space.id} space={space} basePath={basePath} />
            ))}
          </div>
          {/* Planche en bois sous la rangée */}
          <div
            aria-hidden="true"
            className="h-2 rounded bg-gradient-to-b from-amber-700 via-amber-800 to-amber-950 shadow-md"
          />
        </div>
      )}
    </section>
  );
}
