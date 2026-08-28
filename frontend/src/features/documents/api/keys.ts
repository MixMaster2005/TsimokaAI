export const documentsKeys = {
  bySpace: (spaceId: string) => ['documents', 'space', spaceId] as const,
};

export const documentKeys = {
  byId: (documentId: string) => ['documents', documentId] as const,
};
