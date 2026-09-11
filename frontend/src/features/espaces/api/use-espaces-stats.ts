import { useQueries } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';
import { ficheKeys } from '@/features/fiches/api/keys';
import type { Space, MembreEspace } from '../types';
import type { Fiche } from '@/features/fiches/types';

export interface EspaceStats {
  space: Space;
  nbEtudiants: number;
  nbFiches: number;
  nbFichesEnAttente: number;
}

/**
 * Agrège les stats de tous les espaces pour le tableau de bord enseignant.
 * Parallelise les appels membres + fiches pour chaque espace.
 */
export function useEspacesStats(espaces: Space[] | undefined) {
  const spaceIds = espaces?.map((s) => s.id) ?? [];

  const membresResults = useQueries({
    queries: spaceIds.map((id) => ({
      queryKey: espaceKeys.membres(id),
      queryFn: () => apiClient.get<MembreEspace[]>(`/api/v1/spaces/${id}/membres`),
      enabled: spaceIds.length > 0,
    })),
  });

  const fichesResults = useQueries({
    queries: spaceIds.map((id) => ({
      queryKey: ficheKeys.forSpace(id),
      queryFn: () => apiClient.get<Fiche[]>(`/api/v1/fiches/espace/${id}`),
      enabled: spaceIds.length > 0,
    })),
  });

  if (!espaces || spaceIds.length === 0) {
    return { stats: [] as EspaceStats[], isLoading: false };
  }

  const isLoading = membresResults.some((r) => r.isLoading) || fichesResults.some((r) => r.isLoading);

  const stats: EspaceStats[] = espaces.map((space, i) => {
    const membres = membresResults[i]?.data;
    const fiches = fichesResults[i]?.data;

    return {
      space,
      nbEtudiants: membres?.length ?? 0,
      nbFiches: fiches?.length ?? 0,
      nbFichesEnAttente: fiches?.filter((f) => !f.obsolete).length ?? 0,
    };
  });

  return { stats, isLoading };
}
