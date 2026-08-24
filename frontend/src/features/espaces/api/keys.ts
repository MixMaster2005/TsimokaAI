/**
 * Centralise toutes les query keys de la feature — évite les strings
 * magiques dispersées et garantit que les invalidations (après création/
 * suppression d'un espace) retombent exactement sur les bonnes queries.
 */
export const espaceKeys = {
  all: ['espaces'] as const,
  mine: () => [...espaceKeys.all, 'mine'] as const,
  detail: (id: string) => [...espaceKeys.all, 'detail', id] as const,
};
