export const ficheKeys = {
  all: ['fiches'] as const,
  bySpace: (spaceId: string) => [...ficheKeys.all, 'space', spaceId] as const,
  /** Vue enseignant : toutes les fiches d'un espace (GET /fiches/espace/{id}) */
  forSpace: (spaceId: string) => [...ficheKeys.all, 'space', spaceId, 'all'] as const,
  mine: () => [...ficheKeys.all, 'mine'] as const,
  detail: (id: string) => [...ficheKeys.all, 'detail', id] as const,
  shares: (id: string) => [...ficheKeys.all, 'shares', id] as const,
  annotations: (id: string) => [...ficheKeys.all, 'annotations', id] as const,
  validation: (id: string) => [...ficheKeys.all, 'validation', id] as const,
};
