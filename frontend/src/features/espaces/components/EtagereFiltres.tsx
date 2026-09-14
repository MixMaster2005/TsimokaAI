import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import type { FiltreOwner, FiltresEtagere } from '../lib/filtrer-espaces';

interface EtagereFiltresProps {
  value: FiltresEtagere;
  onChange: (v: FiltresEtagere) => void;
  tags: string[];
  showOwnerFilter?: boolean;
}

const SELECT_CLASSNAME =
  'h-9 max-w-xs rounded-md border border-papier-border bg-papier-carte px-3 py-1 text-sm text-encre shadow-xs outline-none transition-colors focus-visible:ring-2 focus-visible:ring-ring';

const OWNER_OPTIONS: { value: FiltreOwner; label: string }[] = [
  { value: 'tous', label: 'Tous' },
  { value: 'miens', label: 'Mes espaces' },
  { value: 'autres', label: 'Autres' },
];

/**
 * Barre de filtres de l'étagère : recherche, tag, tri + segmented control
 * propriétaire (optionnel). Uniquement shadcn/ui + tailwind existants
 * (Input/Button natifs, selects natifs stylisés — pas de nouvelle dépendance).
 */
export function EtagereFiltres({
  value,
  onChange,
  tags,
  showOwnerFilter = false,
}: EtagereFiltresProps) {
  return (
    <div className="flex flex-wrap items-center gap-3 px-8 py-3">
      <Input
        type="search"
        placeholder="Rechercher un espace…"
        aria-label="Rechercher un espace"
        value={value.recherche}
        onChange={(e) => onChange({ ...value, recherche: e.target.value })}
        className="max-w-xs"
      />

      <select
        aria-label="Filtrer par tag"
        value={value.tag}
        onChange={(e) => onChange({ ...value, tag: e.target.value })}
        className={SELECT_CLASSNAME}
      >
        <option value="">Tous les tags</option>
        {tags.map((tag) => (
          <option key={tag} value={tag}>
            {tag}
          </option>
        ))}
      </select>

      <select
        aria-label="Trier les espaces"
        value={value.tri}
        onChange={(e) =>
          onChange({ ...value, tri: e.target.value as FiltresEtagere['tri'] })
        }
        className={SELECT_CLASSNAME}
      >
        <option value="recent">Récents</option>
        <option value="nom">Nom A-Z</option>
        <option value="activite">Activité</option>
      </select>

      {showOwnerFilter && (
        <div
          role="group"
          aria-label="Filtrer par propriétaire"
          className="inline-flex items-center gap-1 rounded-md border border-papier-border bg-papier-carte p-1"
        >
          {OWNER_OPTIONS.map((option) => (
            <Button
              key={option.value}
              type="button"
              size="sm"
              variant={value.owner === option.value ? 'default' : 'ghost'}
              aria-pressed={value.owner === option.value}
              onClick={() => onChange({ ...value, owner: option.value })}
              className={cn(value.owner !== option.value && 'text-encre-muted')}
            >
              {option.label}
            </Button>
          ))}
        </div>
      )}
    </div>
  );
}
