import { queryOptions } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Partage } from '../types';

/** Partages existants d'une fiche (qui l'a reçue / quel groupe). */
export const sharesQueryOptions = (ficheId: string) =>
  queryOptions({
    queryKey: ficheKeys.shares(ficheId),
    queryFn: () => apiClient.get<Partage[]>(`/api/v1/fiches/${ficheId}/share`),
  });
