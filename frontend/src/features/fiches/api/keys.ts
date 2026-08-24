export const ficheKeys = {
  all: ['fiches'] as const,
  bySpace: (spaceId: string) => [...ficheKeys.all, 'space', spaceId] as const,
  detail: (id: string) => [...ficheKeys.all, 'detail', id] as const,
};
