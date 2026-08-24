import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import type { MembreEspace } from '../types';

/**
 * Membres d'un espace (hors propriétaire). Visible par le propriétaire ET les membres.
 * Le nom lisible des utilisateurs n'est pas résoluble côté back aujourd'hui
 * (pas d'endpoint batch user-service) — l'UI affiche un identifiant tronqué.
 */
export const membresQueryOptions = (espaceId: string) =>
  queryOptions({
    queryKey: espaceKeys.membres(espaceId),
    queryFn: () => apiClient.get<MembreEspace[]>(`/api/v1/spaces/${espaceId}/membres`),
  });

export function useMembres(espaceId: string) {
  return useQuery(membresQueryOptions(espaceId));
}
