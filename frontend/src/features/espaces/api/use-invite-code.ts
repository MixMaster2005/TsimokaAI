import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

/**
 * Code d'invitation d'un espace — endpoint réservé au PROPRIÉTAIRE côté back
 * (403 sinon) : `enabled` reste false tant que le rôle n'est pas confirmé,
 * pour ne pas émettre une requête vouée à échouer.
 */
export function useInviteCode(espaceId: string, isOwner: boolean) {
  return useQuery({
    queryKey: espaceKeys.inviteCode(espaceId),
    queryFn: () =>
      apiClient.get<{ inviteCode: string }>(`/api/v1/spaces/${espaceId}/invite-code`),
    enabled: isOwner,
  });
}

/** Régénère le code (le précédent cesse de fonctionner) — propriétaire uniquement. */
export function useRegenerateInviteCode(espaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      apiClient.post<{ inviteCode: string }>(`/api/v1/spaces/${espaceId}/invite-code/regenerate`),
    onSuccess: (data) => {
      // On écrit directement le nouveau code dans le cache : la mutation RENVOIE
      // la valeur, inutile de refetcher juste pour ça.
      queryClient.setQueryData(espaceKeys.inviteCode(espaceId), data);
    },
  });
}

/** Retrait d'un membre par le propriétaire. */
export function useRemoveMembre(espaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberId: string) =>
      apiClient.delete<void>(`/api/v1/spaces/${espaceId}/membres/${memberId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: espaceKeys.membres(espaceId) });
    },
  });
}

/** Un membre quitte l'espace de lui-même. */
export function useLeaveEspace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (espaceId: string) => apiClient.delete<void>(`/api/v1/spaces/${espaceId}/membres/me`),
    onSuccess: () => {
      // L'espace quitte "mes espaces" -> on invalide tout ce qui en dépend.
      queryClient.invalidateQueries({ queryKey: espaceKeys.all });
    },
  });
}
