import type { Space } from '../types';

export type TriEtagere = 'recent' | 'nom' | 'activite';
export type FiltreOwner = 'tous' | 'miens' | 'autres';

export interface FiltresEtagere {
  recherche: string;
  tag: string;
  tri: TriEtagere;
  owner: FiltreOwner;
}

export const FILTRES_ETAGERE_DEFAUT: FiltresEtagere = {
  recherche: '',
  tag: '',
  tri: 'recent',
  owner: 'tous',
};

function estMien(space: Space, currentUserId?: string): boolean {
  return space.owner === true || (currentUserId !== undefined && space.userId === currentUserId);
}

function timestamp(value: string | null | undefined): number {
  if (!value) return 0;
  const t = new Date(value).getTime();
  return Number.isNaN(t) ? 0 : t;
}

/**
 * Filtre + trie une liste d'espaces pour l'étagère.
 * - recherche : nom + description, insensible à la casse ('' = pas de filtre)
 * - tag : correspondance exacte sur subjectTag ('' = tous)
 * - owner : 'miens' = owner===true OU userId===currentUserId, 'autres' = inverse
 * - tri : 'recent' = updatedAt desc, 'nom' = localeCompare fr, 'activite' = lastActivityAt ?? updatedAt ?? createdAt desc
 */
export function filtrerEspaces(
  espaces: Space[],
  filtres: FiltresEtagere,
  currentUserId?: string,
): Space[] {
  const recherche = filtres.recherche.trim().toLowerCase();

  const filtres_ = espaces.filter((space) => {
    if (recherche) {
      const haystack = `${space.name} ${space.description ?? ''}`.toLowerCase();
      if (!haystack.includes(recherche)) return false;
    }
    if (filtres.tag !== '' && space.subjectTag !== filtres.tag) return false;
    if (filtres.owner === 'miens' && !estMien(space, currentUserId)) return false;
    if (filtres.owner === 'autres' && estMien(space, currentUserId)) return false;
    return true;
  });

  const tries = [...filtres_];
  switch (filtres.tri) {
    case 'nom':
      tries.sort((a, b) => a.name.localeCompare(b.name, 'fr'));
      break;
    case 'activite':
      tries.sort(
        (a, b) =>
          timestamp(b.lastActivityAt ?? b.updatedAt ?? b.createdAt) -
          timestamp(a.lastActivityAt ?? a.updatedAt ?? a.createdAt),
      );
      break;
    case 'recent':
    default:
      tries.sort((a, b) => timestamp(b.updatedAt) - timestamp(a.updatedAt));
      break;
  }
  return tries;
}

/**
 * Regroupe les espaces par propriétaire (clé = userId).
 * Prépare le N-étages V2 — regroupement seul, sans affichage.
 */
export function groupSpacesByOwner(espaces: Space[]): Map<string, Space[]> {
  const groupes = new Map<string, Space[]>();
  for (const space of espaces) {
    const liste = groupes.get(space.userId);
    if (liste) {
      liste.push(space);
    } else {
      groupes.set(space.userId, [space]);
    }
  }
  return groupes;
}

/** Tags uniques (non vides) triés en français. */
export function extraireTags(espaces: Space[]): string[] {
  const uniques = new Set<string>();
  for (const space of espaces) {
    const tag = space.subjectTag?.trim();
    if (tag) uniques.add(tag);
  }
  return [...uniques].sort((a, b) => a.localeCompare(b, 'fr'));
}
