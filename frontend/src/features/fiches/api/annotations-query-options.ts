import { queryOptions } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Annotation } from '../types';

export const annotationsQueryOptions = (ficheId: string) =>
  queryOptions({
    queryKey: ficheKeys.annotations(ficheId),
    queryFn: () => apiClient.get<Annotation[]>(`/api/v1/fiches/${ficheId}/annotations`),
  });
