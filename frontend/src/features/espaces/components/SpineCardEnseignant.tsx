import { SpineCard } from './SpineCard';
import type { Space } from '../types';

interface SpineCardEnseignantProps {
  space: Space;
}

/**
 * Variante enseignant du "dos de reliure" : simple wrapper qui délègue à
 * SpineCard avec basePath='enseignant' (pointe vers
 * `/enseignant/espaces/$spaceId`). Gardé pour compat avec les imports existants.
 */
export function SpineCardEnseignant({ space }: SpineCardEnseignantProps) {
  return <SpineCard space={space} basePath="enseignant" />;
}
