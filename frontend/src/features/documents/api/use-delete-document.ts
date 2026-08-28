import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';

export function useDeleteDocument(spaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) =>
      apiClient.delete<void>(`/api/v1/documents/${documentId}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: documentsKeys.bySpace(spaceId) }),
  });
}
