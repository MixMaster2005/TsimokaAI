import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { ficheKeys } from './keys';
import type { Fiche, GenerateFichePayload } from '../types';

/**
 * Génération Map-Reduce côté back (FicheGenerationService) — peut prendre
 * plusieurs secondes selon la taille du corpus. Le composant appelant doit
 * afficher un état de chargement explicite (pas juste un bouton disabled),
 * voir FicheGenerateModal.
 */
export function useGenerateFiche() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: GenerateFichePayload) => apiClient.post<Fiche>('/api/v1/fiches/generate', payload),
    onSuccess: (fiche) => {
      queryClient.invalidateQueries({ queryKey: ficheKeys.bySpace(fiche.spaceId) });
    },
  });
}
