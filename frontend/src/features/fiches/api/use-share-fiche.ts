import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Partage } from '../types';

/**
 * Partage une fiche à un groupe OU un destinataire (exactement l'un des deux,
 * contrôle BadRequest côté back). Seul le PROPRIÉTAIRE de la fiche peut partager.
 */
export function useShareFiche(ficheId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: { groupeId?: string; destinataireId?: string }) =>
      apiClient.post<Partage>(`/api/v1/fiches/${ficheId}/share`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ficheKeys.shares(ficheId) });
    },
  });
}
