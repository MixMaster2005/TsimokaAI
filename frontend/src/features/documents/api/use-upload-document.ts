import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { documentsKeys } from './keys';
import type { AppDocument } from '../types';

export function useUploadDocument(spaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      // spaceId est un @RequestParam côté back (pas dans le form-data) -> en query string
      return apiClient.post<AppDocument>(`/api/v1/documents?spaceId=${spaceId}`, formData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentsKeys.bySpace(spaceId) });
    },
  });
}
