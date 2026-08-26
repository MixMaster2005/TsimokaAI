import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './use-documents';
import type { AppDocument } from '../types';

export function useRetryDocument(spaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (documentId: string) =>
      apiClient.post<AppDocument>(`/api/v1/documents/${documentId}/retry`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentsKeys.bySpace(spaceId) });
    },
  });
}
