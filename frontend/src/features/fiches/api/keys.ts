export const ficheKeys = {
  all: ['fiches'] as const,
  bySpace: (spaceId: string) => [...ficheKeys.all, 'space', spaceId] as const,
  mine: () => [...ficheKeys.all, 'mine'] as const, // vue transverse, tous espaces confondus
  detail: (id: string) => [...ficheKeys.all, 'detail', id] as const,
  shares: (id: string) => [...ficheKeys.all, 'shares', id] as const,
  annotations: (id: string) => [...ficheKeys.all, 'annotations', id] as const,
  validation: (id: string) => [...ficheKeys.all, 'validation', id] as const,
};
